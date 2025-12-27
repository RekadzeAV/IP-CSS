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
import { EventsChart, CamerasChart } from '@/components/Charts';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { fetchEvents, fetchStatistics } from '@/store/slices/eventsSlice';

function DashboardContent() {
  const dispatch = useAppDispatch();
  const { cameras, loading: camerasLoading } = useAppSelector((state) => state.cameras);
  const { events, statistics } = useAppSelector((state) => state.events);

  useEffect(() => {
    dispatch(fetchCameras());
    dispatch(fetchEvents({ limit: 100 }));
    dispatch(fetchStatistics({}));
  }, [dispatch]);

  const onlineCameras = cameras.filter((c) => c.status === 'ONLINE').length;
  const totalEvents = statistics?.total || events.length;
  const totalRecordings = 0; // TODO: добавить когда будет recordingsSlice
  const loading = camerasLoading;

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

          <Grid container spacing={3} sx={{ mt: 2 }}>
            <Grid item xs={12} md={6}>
              <EventsChart events={events} />
            </Grid>
            <Grid item xs={12} md={6}>
              <CamerasChart cameras={cameras} />
            </Grid>
          </Grid>

          <Paper sx={{ p: 3, mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              Недавние события
            </Typography>
            {events.length > 0 ? (
              <Box sx={{ mt: 2 }}>
                {events.slice(0, 5).map((event) => (
                  <Box
                    key={event.id}
                    sx={{
                      p: 1.5,
                      mb: 1,
                      borderRadius: 1,
                      backgroundColor: 'action.hover',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                  >
                    <Box>
                      <Typography variant="body2" fontWeight="medium">
                        {event.cameraName || event.cameraId}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {event.type} • {new Date(event.timestamp).toLocaleString('ru-RU')}
                      </Typography>
                    </Box>
                    <Typography
                      variant="caption"
                      sx={{
                        px: 1,
                        py: 0.5,
                        borderRadius: 1,
                        backgroundColor:
                          event.severity === 'CRITICAL' || event.severity === 'ERROR'
                            ? 'error.main'
                            : event.severity === 'WARNING'
                              ? 'warning.main'
                              : 'info.main',
                        color: 'white',
                      }}
                    >
                      {event.severity}
                    </Typography>
                  </Box>
                ))}
              </Box>
            ) : (
              <Typography variant="body2" color="text.secondary">
                События отсутствуют
              </Typography>
            )}
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

