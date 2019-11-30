import React from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import {Button} from '@material-ui/core';
import {fetchTransaction} from '../../api';
import TransactionDetailsComponent from "./TransactionDetailsComponent";
import Loading from "../Loading";
import SearchBar from 'material-ui-search-bar';
import BitcoinIcon from './BitcoinIcon';

export default class TransactionContainer extends React.Component {
  componentWillMount() {
    this.setState({
      transactionId: this.props.transactionId,
      transaction: undefined
    });
  }

  componentDidMount() {
    if (this.props.transactionId) {
      this.loadTransaction(this.props.transactionId);
    }
  }

  componentWillReceiveProps(nextProps, _) {
    if (this.props.transactionId !== nextProps.transactionId) {
      this.loadTransaction(nextProps.transactionId);
    }
  }

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
      transactionId: transactionId,
      loading: true
    });

    fetchTransaction(transactionId)
      .then((transaction) => {
        this.setState({
          ...this.state,
          loading: false,
          error: undefined,
          transaction: transaction
        });
      })
      .catch((error) => {
        this.setState({
          ...this.state,
          loading: false,
          error: error.response
        });
      });
  };

  showTransactionDetails = () => {
    if (this.state.error) {
      if (this.state.error.status === 404) {
        return (
          <div style={ {marginTop: '32px', textAlign: 'center'} }> 404, <BitcoinIcon style={{verticalAlign: "middle", fontSize: "200px"}}/> transaction not found </div>
        );
      } else {
        return <div style={ {marginTop: '32px', textAlign: 'center'} }>{this.state.error.status},  {this.state.error.statusText}</div>;
      }
    } else if (this.state.transaction) {
      return <TransactionDetailsComponent transaction={this.state.transaction} />;
    } else {
      return null;
    }
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
                (
                  <span>
                    <Button variant="contained" disabled={ this.state.executingScript } onClick={ () =>
                      this.loadTransaction(this.state.transactionId)
                    }>
                      Search
                    </Button>

                    <Button variant="contained" disabled={ this.state.executingScript } onClick={ () =>
                      this.props.push("/safello")
                    }>
                      Buy BTC
                    </Button>
                  </span>
                )

            }
          </div>
          {
            this.showTransactionDetails()
          }
        </div>
      </div>
    );
  }
}