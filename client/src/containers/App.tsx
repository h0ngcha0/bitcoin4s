import React, { Component } from 'react';
import { createTheme, MuiThemeProvider } from '@material-ui/core/styles';
import blue from '@material-ui/core/colors/blue';
import Helmet from 'react-helmet';
import 'flexboxgrid';
import AppRouter from './AppRouter';

const theme = createTheme({
    palette: {
        primary: blue
    }
});

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
                <MuiThemeProvider theme={theme}>
                    <AppRouter />
                </MuiThemeProvider>
            </React.Fragment>
        );
    }
}

export default App;
