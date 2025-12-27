'use client';

import React from 'react';
import { IconButton, Tooltip } from '@mui/material';
import { Brightness4, Brightness7 } from '@mui/icons-material';
import { useTheme } from '@mui/material/styles';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { toggleTheme } from '@/store/slices/settingsSlice';

export default function ThemeSwitcher() {
  const theme = useTheme();
  const dispatch = useAppDispatch();
  const { themeMode } = useAppSelector((state) => state.settings);

  const handleToggle = () => {
    dispatch(toggleTheme());
    // Сохраняем в localStorage
    const newMode = themeMode === 'light' ? 'dark' : 'light';
    localStorage.setItem('themeMode', newMode);
  };

  return (
    <Tooltip title={themeMode === 'light' ? 'Темная тема' : 'Светлая тема'}>
      <IconButton onClick={handleToggle} color="inherit">
        {themeMode === 'light' ? <Brightness4 /> : <Brightness7 />}
      </IconButton>
    </Tooltip>
  );
}

