import {BrowserRouter, Redirect, Route, Switch} from 'react-router-dom';
import React from 'react';
import InterpreterContainer from '../modules/interpreter/InterpreterContainer';
import TransactionContainer from '../modules/transaction/TransactionContainer';

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Switch>
        <Route exact path="/transaction/:transactionId/input/:inputIndex/interpret" render={ ({match: {params: {transactionId, inputIndex}}, history}) =>
          (<InterpreterContainer transactionId={transactionId} inputIndex={inputIndex} push={history.push}/>)
        } />
        <Route exact path="/transaction" render={() => (<TransactionContainer />)} />
        <Route exact path="/transaction/:transactionId" render={ ({match: {params: {transactionId}}}) =>
          (<TransactionContainer transactionId={transactionId} />)
        } />
        <Route exact path="/interpreter" render={() => (<InterpreterContainer />)} />
        <Redirect exact from='/' to='/transaction/f2f398dace996dab12e0cfb02fb0b59de0ef0398be393d90ebc8ab397550370b' />
      </Switch>
    </BrowserRouter>
  );
};

export default AppRouter;
