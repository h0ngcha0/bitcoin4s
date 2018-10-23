import _ from 'lodash';
import React from 'react';
import {findElementType} from './ScriptElements';

class InterpreterComponent extends React.Component {

  state = {
    isVisible: true
  };

  render() {
    const {interpretResult} = this.props;
    const {scriptPubKey, scriptSig, currentScript, stack, altStack, stage} = interpretResult.state;
    const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';
    const executionDescription = result === 'NoResult' ? `Executing ${stage.type}` : `Execution finished with result: ${result}`;

    return (
      <div>
        <p><i>{executionDescription}</i></p>
        <p><b>Current Script:</b></p>
        <ScriptOpCodeList opCodes={currentScript} />
        <p><b>Current Stack:</b></p>
        <ScriptOpCodeList opCodes={stack} />
        <p><b>Current Alt Stack:</b></p>
        <ScriptOpCodeList opCodes={altStack} />
        <p><b>ScriptPubKey:</b></p>
        <ScriptOpCodeList opCodes={scriptPubKey} />
        <p><b>ScriptSig:</b></p>
        <ScriptOpCodeList opCodes={scriptSig} />

      </div>
    )
  }
};

const ScriptOpCodeList = ({opCodes}) => {
  return (
    <div className='ScriptOpCodeList'>
      {
        _(opCodes)
          .filter((opCode) => opCode.type !== 'OP_PUSHDATA')
          .map((scriptElement, index) => {
            const elementType = findElementType(scriptElement.type);
            const className = `OpCode ${elementType}`

            return (
              <div className={ className } key={index}>
                { _.includes(['ScriptConstant', 'ScriptNum'], scriptElement.type) ? scriptElement.value : scriptElement.type }
              </div>
            );
          }).value()
      }
    </div>
  )
};

export default InterpreterComponent;