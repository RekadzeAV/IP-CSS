/** @jest-environment node */

import { NextResponse } from 'next/server';
import { middleware } from './middleware';

describe('middleware', () => {
  it('adds nonce to request and response headers', () => {
    const request = new Request('http://localhost/');

    const response = middleware(request as Parameters<typeof middleware>[0]);
    const nonce = response.headers.get('x-nonce');

    expect(response).toBeInstanceOf(NextResponse);
    expect(nonce).toBeTruthy();
    expect(nonce).toMatch(/^[0-9a-f]{32}$/);
  });
});
