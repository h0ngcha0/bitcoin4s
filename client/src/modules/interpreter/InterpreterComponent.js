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
    const {scriptPubKey, currentScript, stack, altStack, stage} = interpretResult.state;
    const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';

    const currentRemainingScript = () => {
      if (result === 'NoResult') {
        if (stage.type === 'ExecutingScriptSig') {
          return currentScript.concat(scriptPubKey);
        } else {
          return currentScript;
        }
      } else {
        return currentScript;
      }
    };

    const executionDescriptionComponent = (scriptType) => {
      return <span><span style={{textDecoration: "underline"}}>Executing <span style={{fontWeight: "bold"}}>{scriptType}</span></span> [{this.props.step}]</span>;
    };

    const executionDescription = () => {
      if (result === 'NoResult') {
        if (stage.type === 'ExecutingScriptSig') {
          return executionDescriptionComponent("Script Sig");
        } else if (stage.type === 'ExecutingScriptPubKey') {
          return executionDescriptionComponent("Script PubKey");
        } else if (stage.type === 'ExecutingScriptP2SH') {
          return executionDescriptionComponent("Script P2SH");
        } else if (stage.type === 'ExecutingScriptWitness') {
          return executionDescriptionComponent("Script Witness");
        } else {
          return executionDescriptionComponent(stage.type);
        }
      } else {
        return result === 'True' ?
          <span style={{color: "green", textDecoration: "underline"}}>Execution Succeeded</span> :
          <span style={{color: "red", textDecoration: "underline"}}>Execution Failed</span>;
      }
    };

    return (
      <div style={ {maxWidth: '480px', margin: '0 auto'} }>
        <div style={ {textAlign: 'center', marginTop: '20px', fontSize: '13px'} }> {executionDescription()} </div>
        <Table padding="none">
          <TableHead>
            <TableRow>
              <TableCell>Stack</TableCell>
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
              <TableCell>Script:</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            <TableRow>
              <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                <div style={ {marginTop: "5px"}}>
                  <ScriptOpCodeList opCodes={currentRemainingScript()} />
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
      </div>
    )
  }
};

export default InterpreterComponent;