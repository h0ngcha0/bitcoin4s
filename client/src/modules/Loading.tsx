import React from 'react'
import { CircularProgress } from '@mui/material'

function Loading() {
  return (
    <div style={{ width: '100%', display: 'flex', justifyContent: 'center', marginTop: '48px' }}>
      <CircularProgress variant="indeterminate" />
    </div>
  )
}

export default Loading
