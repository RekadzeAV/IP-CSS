'use client';

import React, { useState } from 'react';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  Typography,
  Divider,
  IconButton,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  useTheme,
  useMediaQuery,
  Badge,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  Videocam as VideocamIcon,
  Event as EventIcon,
  VideoLibrary as VideoLibraryIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
  Circle as CircleIcon,
  Notifications as NotificationsIcon,
  People as PeopleIcon,
} from '@mui/icons-material';
import { useRouter, usePathname } from 'next/navigation';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { logout } from '@/store/slices/authSlice';
import ThemeSwitcher from '@/components/ThemeSwitcher/ThemeSwitcher';
import SearchBar from '@/components/SearchBar/SearchBar';

const DRAWER_WIDTH = 240;

interface LayoutProps {
  children: React.ReactNode;
}

const getMenuItems = (isAdmin: boolean) => [
  { text: 'Главная', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Камеры', icon: <VideocamIcon />, path: '/cameras' },
  { text: 'События', icon: <EventIcon />, path: '/events' },
  { text: 'Записи', icon: <VideoLibraryIcon />, path: '/recordings' },
  { text: 'Уведомления', icon: <NotificationsIcon />, path: '/notifications' },
  ...(isAdmin ? [{ text: 'Пользователи', icon: <PeopleIcon />, path: '/users' }] : []),
  { text: 'Настройки', icon: <SettingsIcon />, path: '/settings' },
];

export default function Layout({ children }: LayoutProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const router = useRouter();
  const pathname = usePathname();
  const dispatch = useAppDispatch();
  const { connected, connecting } = useAppSelector((state) => state.websocket);
  const { unreadCount } = useAppSelector((state) => state.notifications);
  const { user } = useAppSelector((state) => state.auth);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleLogout = async () => {
    await dispatch(logout());
    router.push('/login');
  };

  const drawer = (
    <Box>
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          IP Camera System
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {getMenuItems(user?.role === 'ADMIN').map((item) => (
          <ListItem key={item.path} disablePadding>
            <ListItemButton
              selected={pathname === item.path}
              onClick={() => {
                router.push(item.path);
                if (isMobile) setMobileOpen(false);
              }}
            >
              <ListItemIcon>
                {item.path === '/notifications' ? (
                  <Badge badgeContent={unreadCount} color="error">
                    {item.icon}
                  </Badge>
                ) : (
                  item.icon
                )}
              </ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Divider />
      <List>
        <ListItem disablePadding>
          <ListItemButton onClick={handleLogout}>
            <ListItemIcon>
              <LogoutIcon />
            </ListItemIcon>
            <ListItemText primary="Выход" />
          </ListItemButton>
        </ListItem>
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { md: `${DRAWER_WIDTH}px` },
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Система видеонаблюдения
          </Typography>
          <Box sx={{ flexGrow: 1, maxWidth: 400, mx: 2, display: { xs: 'none', md: 'block' } }}>
            <SearchBar placeholder="Поиск камер, событий, записей..." />
          </Box>
          <ThemeSwitcher />
          <Tooltip
            title={
              connecting
                ? 'Подключение к WebSocket...'
                : connected
                  ? 'WebSocket подключен'
                  : 'WebSocket отключен'
            }
          >
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                mr: 2,
                color: connecting ? 'warning.main' : connected ? 'success.main' : 'error.main',
              }}
            >
              <CircleIcon sx={{ fontSize: 12, mr: 0.5 }} />
              <Typography variant="caption" sx={{ display: { xs: 'none', sm: 'block' } }}>
                {connecting ? 'Подключение...' : connected ? 'Онлайн' : 'Офлайн'}
              </Typography>
            </Box>
          </Tooltip>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: DRAWER_WIDTH,
            },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: DRAWER_WIDTH,
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
        }}
      >
        {children}
      </Box>
    </Box>
  );
}


