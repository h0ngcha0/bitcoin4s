import React, { Component } from 'react';
import {
    ThemeProvider,
    Theme,
    StyledEngineProvider,
    createMuiTheme,
    createTheme,
    makeStyles,
    adaptV4Theme,
} from '@mui/material/styles';
import blue from '@mui/material/colors/blue';
import Helmet from 'react-helmet';
import 'flexboxgrid';
import AppRouter from './AppRouter';

declare module '@mui/styles/defaultTheme' {
  // eslint-disable-next-line @typescript-eslint/no-empty-interface
  interface DefaultTheme extends Theme {}
}

//const theme = createMuiTheme();

const theme = createTheme(adaptV4Theme({
    palette: {
        primary: blue
    }
}));

class App extends Component {
    state = {};

    render() {
        return (
            <React.Fragment>
                <Helmet
                    key="helmet"
                    defaultTitle="bitcoin.reverse"
                    titleTemplate="%s - bitcoin.reverse"
                />,
                <StyledEngineProvider injectFirst>
                    <ThemeProvider theme={theme}>
                        <AppRouter />
                    </ThemeProvider>
                </StyledEngineProvider>
            </React.Fragment>
        );
    }
}

export default App;
