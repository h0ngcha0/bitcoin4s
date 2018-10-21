import React, {Component} from 'react';
import {MuiThemeProvider} from 'material-ui';
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
          <AppRouter />
      </MuiThemeProvider>
    ];
  }
}

export default App;
