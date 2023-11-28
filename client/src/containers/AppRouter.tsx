import { HashRouter, Redirect, Route, Switch } from 'react-router-dom'
import React from 'react'
import InterpreterContainer from '../modules/interpreter/InterpreterContainer'
import { TransactionContainer } from '../modules/transaction/TransactionContainer'
import qs from 'qs'

const AppRouter = () => {
  return (
    <HashRouter>
      <Switch>
        <Route
          exact
          path="/transaction/:transactionId/input/:inputIndex/interpret"
          render={({
            match: {
              params: { transactionId, inputIndex }
            },
            history,
            location
          }) => {
            const queryParams = qs.parse(location.search, { ignoreQueryPrefix: true })
            const automatic = Boolean(queryParams.automatic)
            const step = queryParams.step ? +queryParams.step : 0
            return (
              <InterpreterContainer
                transactionId={transactionId}
                inputIndex={+inputIndex}
                automatic={automatic}
                step={step}
                push={history.push}
              />
            )
          }}
        />
        <Route
          exact
          path="/transaction/:transactionId"
          render={({
            match: {
              params: { transactionId }
            },
            history
          }) => <TransactionContainer transactionId={transactionId} push={history.push} />}
        />
        <Redirect exact from="/" to="/transaction/f2f398dace996dab12e0cfb02fb0b59de0ef0398be393d90ebc8ab397550370b" />
      </Switch>
    </HashRouter>
  )
}

export default AppRouter
