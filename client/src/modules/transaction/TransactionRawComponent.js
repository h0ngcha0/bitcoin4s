import _ from 'lodash';

import React from 'react';
import Typography from "@material-ui/core/Typography";

class TransactionRawComponent extends React.Component {
    render() {
        const {transaction} = this.props;
        const txRaw = transaction.txRaw;

        return (
            <div style={ {maxWidth: '480px', textAlign: 'left', marginTop: '20px', wordWrap: "break-word"} }>
                <Typography color="textSecondary" variant="caption">
                    <span>{ txRaw.version }</span>
                    {
                        txRaw.flag ? (
                            <span>{txRaw.flag}</span>
                        ) : null
                    }
                    {
                        <span>{txRaw.txIns.count}</span>
                    }
                    {
                        _.map(txRaw.txIns.txIns, function(txIn) {
                            return (
                                <span>
                                    <span>{txIn.previousOutput.hash}</span>
                                    <span>{txIn.previousOutput.index}</span>
                                    <span>{txIn.sigScript}</span>
                                    <span>{txIn.sequence}</span>
                                </span>
                            )
                        })
                    }
                    {
                        <span>{txRaw.txOuts.count}</span>
                    }
                    {
                        _.map(txRaw.txOuts.txOuts, function(txOut) {
                            return (
                                <span>
                                    <span>{txOut.value}</span>
                                    <span>{txOut.pkScript}</span>
                                </span>
                            )
                        })
                    }
                    {
                        _.map(txRaw.txWitnesses, function(txWitnesses) {
                            return (
                                <span>
                                    <span>{txWitnesses.count}</span>
                                    {
                                        _.flatMap(txWitnesses.txWitnesses, function (txWitness) {
                                            return (
                                                <span>{txWitness.witness}</span>
                                            )
                                        })
                                    }
                                </span>
                            )
                        })
                    }
                    {
                        <span>{txRaw.lockTime}</span>
                    }
                </Typography>
            </div>
        );
    }
}

export default TransactionRawComponent;