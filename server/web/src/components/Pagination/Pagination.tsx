'use client';

import React from 'react';
import {
  Box,
  Pagination as MuiPagination,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  SelectChangeEvent,
} from '@mui/material';

export interface PaginationProps {
  page: number;
  limit: number;
  total: number;
  onPageChange: (page: number) => void;
  onLimitChange?: (limit: number) => void;
  limits?: number[];
  showLimitSelector?: boolean;
  showTotal?: boolean;
}

export default function Pagination({
  page,
  limit,
  total,
  onPageChange,
  onLimitChange,
  limits = [10, 20, 50, 100],
  showLimitSelector = true,
  showTotal = true,
}: PaginationProps) {
  const totalPages = Math.ceil(total / limit);

  const handlePageChange = (_: React.ChangeEvent<unknown>, value: number) => {
    onPageChange(value);
  };

  const handleLimitChange = (event: SelectChangeEvent<number>) => {
    const newLimit = Number(event.target.value);
    if (onLimitChange) {
      onLimitChange(newLimit);
    }
  };

  if (total === 0) {
    return null;
  }

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: 2,
        mt: 3,
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        {showTotal && (
          <Typography variant="body2" color="text.secondary">
            Всего: {total} | Страница {page} из {totalPages}
          </Typography>
        )}
        {showLimitSelector && onLimitChange && (
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>На странице</InputLabel>
            <Select value={limit} onChange={handleLimitChange} label="На странице">
              {limits.map((l) => (
                <MenuItem key={l} value={l}>
                  {l}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}
      </Box>

      <MuiPagination
        count={totalPages}
        page={page}
        onChange={handlePageChange}
        color="primary"
        showFirstButton
        showLastButton
        size="medium"
      />
    </Box>
  );
}

