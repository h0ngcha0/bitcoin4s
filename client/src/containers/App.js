import React, {Component} from 'react';
import {MuiThemeProvider} from 'material-ui';
import {MuiThemeProvider as MuiThemeProviderNext, createMuiTheme} from 'material-ui-next';
import injectTapEventPlugin from 'react-tap-event-plugin';
import Helmet from 'react-helmet';
import 'flexboxgrid';
import getMuiTheme from 'material-ui/styles/getMuiTheme';
import {blue500, darkWhite, red500, white} from 'material-ui/styles/colors';
import blue from 'material-ui-next/colors/blue';
import AppRouter from './AppRouter';

export const theme = getMuiTheme({
  palette: {
    primary1Color: blue500,
    lightTextColor: white,
    lightSecondaryTextColor: darkWhite
  },
  raisedButton: {
    primaryTextColor: white,
    secondaryColor: red500,
    secondaryTextColor: white
  }
});

const themeNew = createMuiTheme({
  palette: {
    primary: blue
  }
});

injectTapEventPlugin();

class App extends Component {
  state = {};

  render() {
    return [
      <Helmet
        key="helmet"
        defaultTitle="bitcoin.reverse"
        titleTemplate="%s - bitcoin.reverse"
      />,
      <MuiThemeProvider key="theme" muiTheme={theme}>
        <MuiThemeProviderNext theme={themeNew}>
          <AppRouter />
        </MuiThemeProviderNext>
      </MuiThemeProvider>
    ];
  }
}

export default App;
