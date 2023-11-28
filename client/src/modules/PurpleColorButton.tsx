import { withStyles } from 'tss-react/mui';
import { blue, purple } from '@mui/material/colors';
import Button from "@mui/material/Button";

export const PurpleColorButton = withStyles(Button, theme => ({
  root: {
    color: theme.palette.getContrastText(purple[500]),
    backgroundColor: purple[500],
    '&:hover': {
      backgroundColor: purple[700],
    },
  },
}));

export const BlueColorButton = withStyles(Button, theme => ({
  root: {
    color: theme.palette.getContrastText(blue[500]),
    backgroundColor: blue[500],
    '&:hover': {
      backgroundColor: blue[700],
    },
  },
}));
