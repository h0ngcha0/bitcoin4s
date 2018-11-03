import React from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import {Button} from '@material-ui/core';
import {fetchTransaction} from '../../api';
import TransactionDetailsComponent from "./TransactionDetailsComponent";
import Loading from "../Loading";
import SearchBar from 'material-ui-search-bar'

export default class TransactionContainer extends React.Component {
  componentDidMount() {
    if (this.props.transactionId) {
      this.loadTransaction(this.props.transactionId);
    }
  }

  state = {
    transactionId: this.props.transactionId,
    transaction: undefined
  };

  handleSetTransactionId = (txId) => {
    this.setState({
      ...this.state,
      transactionId: txId
    });
  };

  loadTransaction = (transactionId) => {
    this.setState({
      ...this.state,
      transaction: undefined,
      interpretResult: undefined,
      loading: true
    });

    fetchTransaction(transactionId)
      .then((transaction) => {
        this.setState({
          ...this.state,
          loading: false,
          transaction: transaction
        });
      })
      .catch((error) => {
        console.log(error);
      });
  };

  render() {
    return (
      <div className="container">
        <div className={'application-definition'}>
          <img src={ mobileLogoImage } className={`logo-image img-responsive mobile`} alt="Bitcoin Playground"/>
          <img src={ desktopLogoImage } className={`logo-image img-responsive desktop`} alt="Bitcoin Playground"/>
          <SearchBar
            value={this.state.transactionId}
            placeholder="BTC transaction id"
            onChange={(newValue) => this.handleSetTransactionId(newValue)}
            disabled={ this.state.executingScript }
            style={ {maxWidth: '500px', textAlign: 'center', margin: '0 auto'} }
            onRequestSearch={() => {
              this.loadTransaction(this.state.transactionId);
            }}
          />
          <div style={ {marginTop: '16px', textAlign: 'center'} }>
            {
              this.state.loading ?
                <Loading /> :
                <Button variant="contained" disabled={ this.state.executingScript } onClick={ () =>
                  this.loadTransaction(this.state.transactionId)
                  //this.interpretScriptWebsocket(this.state.transactionId, this.state.inputIndex)
                }>
                  Search
                </Button>

            }
          </div>
          {
            this.state.transaction ?
              <TransactionDetailsComponent transaction={this.state.transaction} /> : null
          }
        </div>
      </div>
    );
  }
}