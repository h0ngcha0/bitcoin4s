import React, {Component} from 'react';
import {MuiThemeProvider} from 'material-ui';
import {MuiThemeProvider as MuiThemeProviderNext, createMuiTheme} from 'material-ui-next';
import injectTapEventPlugin from 'react-tap-event-plugin';
import Helmet from 'react-helmet';
import 'flexboxgrid';
import AppRouter from './AppRouter';

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
      <MuiThemeProvider key="theme">
        <MuiThemeProviderNext>
          <AppRouter />
        </MuiThemeProviderNext>
      </MuiThemeProvider>
    ];
  }
}

export default App;
