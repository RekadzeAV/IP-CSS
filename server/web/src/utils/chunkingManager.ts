/**
 * Менеджер для сборки chunked сообщений
 */

const CHUNK_SIZE_THRESHOLD = 64 * 1024; // 64KB
const MAX_CHUNK_SIZE = 64 * 1024; // 64KB
const CHUNK_TIMEOUT_MS = 30_000; // 30 секунд

/**
 * Chunked сообщение в процессе сборки
 */
interface ChunkedMessage {
  messageId: string;
  totalChunks: number;
  metadata: BinaryMessageMetadata;
  receivedChunks: Map<number, Uint8Array>;
  timestamp: number;
  timeoutId?: NodeJS.Timeout;
}

/**
 * Метаданные для бинарных сообщений
 */
export interface BinaryMessageMetadata {
  type: string;
  mimeType: string;
  size: number;
  messageId: string;
  chunkIndex?: number;
  totalChunks?: number;
  timestamp?: number;
}

/**
 * Менеджер для сборки chunked сообщений
 */
export class ChunkingManager {
  private chunks = new Map<string, ChunkedMessage>();

  /**
   * Добавить chunk к сообщению
   */
  addChunk(
    messageId: string,
    chunkIndex: number,
    totalChunks: number,
    data: Uint8Array,
    metadata: BinaryMessageMetadata
  ): Uint8Array | null {
    let chunkedMessage = this.chunks.get(messageId);

    if (!chunkedMessage) {
      chunkedMessage = {
        messageId,
        totalChunks,
        metadata,
        receivedChunks: new Map(),
        timestamp: Date.now(),
      };
      this.chunks.set(messageId, chunkedMessage);
    }

    chunkedMessage.receivedChunks.set(chunkIndex, data);

    // Проверяем, все ли chunks получены
    if (chunkedMessage.receivedChunks.size === totalChunks) {
      const completeData = this.assemble(chunkedMessage);
      this.chunks.delete(messageId);
      if (chunkedMessage.timeoutId) {
        clearTimeout(chunkedMessage.timeoutId);
      }
      console.debug(
        `[ChunkingManager] Assembled message ${messageId} from ${totalChunks} chunks`
      );
      return completeData;
    }

    // Запускаем таймаут для неполных сообщений
    if (!chunkedMessage.timeoutId) {
      chunkedMessage.timeoutId = setTimeout(() => {
        const msg = this.chunks.get(messageId);
        if (msg && msg.receivedChunks.size < totalChunks) {
          console.warn(
            `[ChunkingManager] Chunked message ${messageId} timed out (${msg.receivedChunks.size}/${totalChunks} chunks received)`
          );
          this.chunks.delete(messageId);
        }
      }, CHUNK_TIMEOUT_MS);
    }

    return null;
  }

  /**
   * Собрать chunks в полное сообщение
   */
  private assemble(chunkedMessage: ChunkedMessage): Uint8Array {
    const { receivedChunks, totalChunks } = chunkedMessage;

    if (receivedChunks.size !== totalChunks) {
      throw new Error(
        `Cannot assemble incomplete message: ${receivedChunks.size}/${totalChunks} chunks`
      );
    }

    // Собираем chunks в правильном порядке
    const chunks: Uint8Array[] = [];
    for (let i = 0; i < totalChunks; i++) {
      const chunk = receivedChunks.get(i);
      if (!chunk) {
        throw new Error(`Missing chunk ${i} for message ${chunkedMessage.messageId}`);
      }
      chunks.push(chunk);
    }

    const totalSize = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
    const result = new Uint8Array(totalSize);

    let offset = 0;
    for (const chunk of chunks) {
      result.set(chunk, offset);
      offset += chunk.length;
    }

    return result;
  }

  /**
   * Очистить старые chunks
   */
  cleanup(): void {
    const now = Date.now();
    for (const [messageId, message] of this.chunks.entries()) {
      if (now - message.timestamp > CHUNK_TIMEOUT_MS) {
        if (message.timeoutId) {
          clearTimeout(message.timeoutId);
        }
        this.chunks.delete(messageId);
      }
    }
  }

  /**
   * Отменить таймаут для сообщения
   */
  cancelTimeout(messageId: string): void {
    const message = this.chunks.get(messageId);
    if (message?.timeoutId) {
      clearTimeout(message.timeoutId);
      message.timeoutId = undefined;
    }
  }

  /**
   * Очистить все chunks
   */
  clear(): void {
    for (const message of this.chunks.values()) {
      if (message.timeoutId) {
        clearTimeout(message.timeoutId);
      }
    }
    this.chunks.clear();
  }

  /**
   * Проверить, нужно ли разбивать сообщение на chunks
   */
  static needsChunking(data: Uint8Array): boolean {
    return data.length > CHUNK_SIZE_THRESHOLD;
  }

  /**
   * Разбить данные на chunks
   */
  static splitIntoChunks(data: Uint8Array, messageId: string): Uint8Array[] {
    if (!ChunkingManager.needsChunking(data)) {
      return [data];
    }

    const chunks: Uint8Array[] = [];
    let offset = 0;

    while (offset < data.length) {
      const chunkSize = Math.min(MAX_CHUNK_SIZE, data.length - offset);
      const chunk = data.slice(offset, offset + chunkSize);
      chunks.push(chunk);
      offset += chunkSize;
    }

    console.debug(
      `[ChunkingManager] Split message ${messageId} into ${chunks.length} chunks`
    );
    return chunks;
  }
}
