import _ from 'lodash';

import React from 'react';
import Typography from "@mui/material/Typography";
import Tooltip from "@mui/material/Tooltip";
import { Transaction } from '../../api';

interface TransactionRawComponentProps {
  transaction: Transaction
}

export const TransactionRawComponent: React.FunctionComponent<TransactionRawComponentProps> = (props) => {
  const { transaction } = props;
  const txRaw = transaction.txRaw;

  return txRaw ? (
    <div style={{ maxWidth: '480px', textAlign: 'left', marginTop: '20px', wordWrap: "break-word" }}>
      <Typography variant="body2">
        <Tooltip title="Version" arrow>
          <span className="TxVersion">{txRaw.version}</span>
        </Tooltip>
        {
          txRaw.flag ? (
            <Tooltip title="Flag" arrow>
              <span className="TxFlag">{txRaw.flag}</span>
            </Tooltip>
          ) : undefined
        }
        {
          <Tooltip title="Number of transaction inputs" arrow>
            <span className="TxListCount">{txRaw.txIns.count}</span>
          </Tooltip>
        }
        {
          _.map(txRaw.txIns.txIns, (txIn, index) => {
            return (
              <span>
                <Tooltip title={`Spent transaction id for input ${index}`} arrow>
                  <span className="TxInputPreviousTxId">{txIn.previousOutput.hash}</span>
                </Tooltip>
                <Tooltip title={`Spent transaction output index for input ${index}`} arrow>
                  <span className="TxInputPreviousTxOutput">{txIn.previousOutput.index}</span>
                </Tooltip>
                <Tooltip title={`Unlocking script for input ${index}`} arrow>
                  <span className="TxInputUnLockingScript">{txIn.sigScript}</span>
                </Tooltip>
                <Tooltip title={`Sequence number for input ${index}`} arrow>
                  <span className="TxInputSequence">{txIn.sequence}</span>
                </Tooltip>
              </span>
            )
          })
        }
        {
          <Tooltip title="Number of transaction outputs" arrow>
            <span className="TxListCount">{txRaw.txOuts.count}</span>
          </Tooltip>
        }
        {
          _.map(txRaw.txOuts.txOuts, (txOut, index) => {
            return (
              <span>
                <Tooltip title={`Value of output ${index}`} arrow>
                  <span className="TxOutputValue">{txOut.value}</span>
                </Tooltip>
                <Tooltip title={`Locking script for output ${index}`} arrow>
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
                <Tooltip title={`Number of witnesses for input ${inputIndex}`} arrow>
                  <span className="TxListCount">{txWitnesses.count}</span>
                </Tooltip>
                {
                  _.flatMap(txWitnesses.txWitnesses, (txWitness, witnessIndex) => {
                    return (
                      <Tooltip title={`Witness ${witnessIndex} for input ${inputIndex}`} arrow>
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
          <Tooltip title="Locking time" arrow>
            <span className="TxLockTime">{txRaw.lockTime}</span>
          </Tooltip>
        }
      </Typography>
    </div>
  ) : (<div> Transaction raw data not available </div>);
}

export default TransactionRawComponent;