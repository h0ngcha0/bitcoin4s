import {BrowserRouter, Redirect, Route, Switch} from 'react-router-dom';
import React from 'react';
import InterpreterContainer from '../modules/interpreter/InterpreterContainer';
import TransactionContainer from '../modules/transaction/TransactionContainer';

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Switch>
        <Route exact path="/transaction" render={() => (<TransactionContainer />)} />
        <Route exact path="/transaction/:transactionId" render={ ({match: {params: {transactionId}}}) =>
          (<TransactionContainer transactionId={transactionId} />)
        } />
        <Route exact path="/interpreter" render={() => (<InterpreterContainer />)} />
        <Redirect exact from='/' to='/transaction' />
      </Switch>
    </BrowserRouter>
  );
};

export default AppRouter;
