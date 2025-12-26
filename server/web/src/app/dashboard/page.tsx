'use client';

import React, { useEffect } from 'react';
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  CircularProgress,
} from '@mui/material';
import {
  Videocam as VideocamIcon,
  Event as EventIcon,
  VideoLibrary as VideoLibraryIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCameras } from '@/store/slices/camerasSlice';

function DashboardContent() {
  const dispatch = useAppDispatch();
  const { cameras, loading } = useAppSelector((state) => state.cameras);

  useEffect(() => {
    dispatch(fetchCameras());
  }, [dispatch]);

  const onlineCameras = cameras.filter((c) => c.status === 'ONLINE').length;
  const totalEvents = 0; // TODO: получить из events slice
  const totalRecordings = 0; // TODO: получить из recordings slice

  const stats = [
    {
      title: 'Всего камер',
      value: cameras.length,
      icon: <VideocamIcon />,
      color: '#1976d2',
    },
    {
      title: 'Онлайн',
      value: onlineCameras,
      icon: <CheckCircleIcon />,
      color: '#2e7d32',
    },
    {
      title: 'События',
      value: totalEvents,
      icon: <EventIcon />,
      color: '#ed6c02',
    },
    {
      title: 'Записи',
      value: totalRecordings,
      icon: <VideoLibraryIcon />,
      color: '#9c27b0',
    },
  ];

  return (
    <Layout>
      <Typography variant="h4" gutterBottom>
        Главная панель
      </Typography>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Grid container spacing={3} sx={{ mt: 2 }}>
            {stats.map((stat, index) => (
              <Grid item xs={12} sm={6} md={3} key={index}>
                <Card>
                  <CardContent>
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                      <Box
                        sx={{
                          width: 56,
                          height: 56,
                          borderRadius: 1,
                          backgroundColor: `${stat.color}20`,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          mr: 2,
                          color: stat.color,
                        }}
                      >
                        {stat.icon}
                      </Box>
                      <Box>
                        <Typography variant="h4" component="div">
                          {stat.value}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {stat.title}
                        </Typography>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>

          <Paper sx={{ p: 3, mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              Недавние события
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Здесь будет список последних событий (в разработке)
            </Typography>
          </Paper>
        </>
      )}
    </Layout>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardContent />
    </ProtectedRoute>
  );
}

