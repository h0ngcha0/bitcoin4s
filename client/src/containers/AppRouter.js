import {HashRouter, Redirect, Route, Switch} from 'react-router-dom';
import React from 'react';
import InterpreterContainer from '../modules/interpreter/InterpreterContainer';

const AppRouter = () => {
  return (
    <HashRouter>
      <Switch>
        <Route exact path="/interpreter" render={() => (<InterpreterContainer />)} />
        <Redirect exact from='/' to='/interpreter' />
      </Switch>
    </HashRouter>
  );
};

export default AppRouter;
