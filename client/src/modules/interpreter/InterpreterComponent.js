import React from 'react';

import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import _ from 'lodash';
import ScriptOpCodeList from '../transaction/ScriptOpCodeList';

class InterpreterComponent extends React.Component {

  render() {
    const {interpretResult} = this.props;
    const {scriptPubKey, scriptSig, currentScript, stack, altStack, stage} = interpretResult.state;
    const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';
    const executionDescription = result === 'NoResult' ? `Executing ${stage.type}` : `Execution finished with result: ${result}`;

    return (
      <div style={ {maxWidth: '550px', margin: '0 auto'} }>
        {/*<div style={{fontSize: "12px"}}>{executionDescription}</div>*/}

        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Current Stack</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            <TableRow>
              <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                <div style={ {marginTop: "5px"}}>
                  <ScriptOpCodeList opCodes={stack} />
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Current Script: {executionDescription}</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            <TableRow>
              <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                <div style={ {marginTop: "5px"}}>
                  <ScriptOpCodeList opCodes={currentScript} />
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        {
          !_.isEmpty(altStack) ? (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Current Alt Stack</TableCell>
                </TableRow>
              </TableHead>

              <TableBody>
                <TableRow>
                  <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                    <div style={ {marginTop: "5px"}}>
                      <ScriptOpCodeList opCodes={altStack} />
                    </div>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          ) : null
        }
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ScriptPubKey:</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            <TableRow>
              <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                <div style={ {marginTop: "5px"}}>
                  <ScriptOpCodeList opCodes={scriptPubKey} />
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>

        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ScriptSig:</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            <TableRow>
              <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                <div style={ {marginTop: "5px"}}>
                  <ScriptOpCodeList opCodes={scriptSig} />
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </div>
    )
  }
};

export default InterpreterComponent;