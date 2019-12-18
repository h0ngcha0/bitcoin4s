import _ from 'lodash';

import React from 'react';
import Typography from "@material-ui/core/Typography";
import Tooltip from "@material-ui/core/Tooltip";

class TransactionRawComponent extends React.Component {
    render() {
        const {transaction} = this.props;
        const txRaw = transaction.txRaw;

        return (
            <div style={ {maxWidth: '480px', textAlign: 'left', marginTop: '20px', wordWrap: "break-word"} }>
                <Typography variant="body2">
                    <Tooltip title="Version" arrow interactive>
                        <span className="TxVersion">{ txRaw.version }</span>
                    </Tooltip>
                    {
                        txRaw.flag ? (
                            <Tooltip title="Flag" arrow interactive>
                                <span className="TxFlag">{txRaw.flag}</span>
                            </Tooltip>
                        ) : null
                    }
                    {
                        <Tooltip title="Number of transaction inputs" arrow interactive>
                            <span className="TxListCount">{txRaw.txIns.count}</span>
                        </Tooltip>
                    }
                    {
                        _.map(txRaw.txIns.txIns, (txIn, index) => {
                            return (
                                <span>
                                    <Tooltip title={`Spent transaction id for input ${index}`} arrow interactive>
                                        <span className="TxInputPreviousTxId">{txIn.previousOutput.hash}</span>
                                    </Tooltip>
                                    <Tooltip title={`Spent transaction output index for input ${index}`} arrow interactive>
                                        <span className="TxInputPreviousTxOutput">{txIn.previousOutput.index}</span>
                                    </Tooltip>
                                    <Tooltip title={`Unlocking script for input ${index}`} arrow interactive>
                                        <span className="TxInputUnLockingScript">{txIn.sigScript}</span>
                                    </Tooltip>
                                    <Tooltip title={`Sequence number for input ${index}`} arrow interactive>
                                        <span className="TxInputSequence">{txIn.sequence}</span>
                                    </Tooltip>
                                </span>
                            )
                        })
                    }
                    {
                        <Tooltip title="Number of transaction outputs" arrow interactive>
                            <span className="TxListCount">{txRaw.txOuts.count}</span>
                        </Tooltip>
                    }
                    {
                        _.map(txRaw.txOuts.txOuts, (txOut, index) => {
                            return (
                                <span>
                                    <Tooltip title={`Value of output ${index}`} arrow interactive>
                                        <span className="TxOutputValue">{txOut.value}</span>
                                    </Tooltip>
                                    <Tooltip title={`Locking script for output ${index}`} arrow interactive>
                                        <span className="TxOutputLockingScript">{txOut.pkScript}</span>
                                    </Tooltip>
                                </span>
                            )
                        })
                    }
                    {
                        _.map(txRaw.txWitnesses, (txWitnesses, inputIndex) => {
                            return (
                                <span>
                                    <Tooltip title={`Number of witnesses for input ${inputIndex}`} arrow interactive>
                                        <span className="TxListCount">{txWitnesses.count}</span>
                                    </Tooltip>
                                    {
                                        _.flatMap(txWitnesses.txWitnesses, (txWitness, witnessIndex) => {
                                            return (
                                                <Tooltip title={`Witness ${witnessIndex} for input ${inputIndex}`} arrow interactive>
                                                    <span className={`TxWitness${witnessIndex % 2}`}>{txWitness.witness}</span>
                                                </Tooltip>
                                            )
                                        })
                                    }
                                </span>
                            )
                        })
                    }
                    {
                        <Tooltip title="Locking time" arrow interactive>
                            <span className="TxLockTime">{txRaw.lockTime}</span>
                        </Tooltip>
                    }
                </Typography>
            </div>
        );
    }
}

export default TransactionRawComponent;