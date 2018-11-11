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

    const executionDescription = () => {
      if (result === 'NoResult') {
        if (stage.type === 'ExecutingScriptSig') {
          return <span style={{textDecoration: "underline"}}>Executing <span style={{fontWeight: "bold"}}>Script Sig</span></span>;
        } else if (stage.type === 'ExecutingScriptPubKey') {
          return <span style={{textDecoration: "underline"}}>Executing <span style={{fontWeight: "bold"}}>Script PubKey</span></span>;
        } else if (stage.type === 'ExecutingScriptP2SH') {
          return <span style={{textDecoration: "underline"}}>Executing <span style={{fontWeight: "bold"}}>Script P2SH</span></span>;
        } else if (stage.type === 'ExecutingScriptWitness') {
          return <span style={{textDecoration: "underline"}}>Executing <span style={{fontWeight: "bold"}}>Script Witness</span></span>;
        } else {
          return <span style={{textDecoration: "underline"}}>`Executing <span style={{fontWeight: "bold"}}>${stage.type}`</span></span>;
        }
      } else {
        if (result === 'True') {
          return <span style={{color: "green", textDecoration: "underline"}}>Execution Succeeded</span>;
        } else {
          return <span style={{color: "red", textDecoration: "underline"}}>Execution Failed</span>;
        }

      }
    };

    return (
      <div style={ {maxWidth: '480px', margin: '0 auto'} }>
        <div style={ {textAlign: 'center', marginTop: '20px', fontSize: '13px'} }> {executionDescription()} </div>
        <Table padding="none">
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
        <Table padding="none">
          <TableHead>
            <TableRow>
              <TableCell>Current Script:</TableCell>
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
            <Table padding="none">
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
        <Table padding="none">
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

        <Table padding="none">
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