'use client';

import React from 'react';
import { Box, Typography, Paper } from '@mui/material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';

function RecordingsContent() {
  return (
    <Layout>
      <Typography variant="h4" gutterBottom>
        Записи
      </Typography>
      <Paper sx={{ p: 3, mt: 2 }}>
        <Typography variant="body1" color="text.secondary">
          Страница записей находится в разработке.
        </Typography>
      </Paper>
    </Layout>
  );
}

export default function RecordingsPage() {
  return (
    <ProtectedRoute>
      <RecordingsContent />
    </ProtectedRoute>
  );
}

