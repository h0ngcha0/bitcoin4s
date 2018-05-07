import _ from 'lodash';
import React from 'react';
import VirtualList from 'react-tiny-virtual-list';
import {findElementType} from './ScriptElements';

class InterpreterComponent extends React.Component {

  state = {
    isVisible: true
  };

  render() {

    const {interpretResult} = this.props;

    const {scriptPubKey, scriptSig, currentScript, stack, altStack, stage} = interpretResult.state;
    const getType = (op) => op.type;

    const scriptPubKeyOps = _.map(scriptPubKey, getType);
    //const scriptPubKeyOps = ['OP_DUP', 'OP_CHECKSIG', 'OP_VERIFY', 'ScriptConstant', 'OP_1', 'OP_1', 'OP_0', 'OP_2', 'OP_3', 'OP_4', 'OP_5', 'OP_6'];
    const scriptSigOps = _.map(scriptSig, getType);
    const currentScriptOps = _.map(currentScript, getType);
    const stackOps = _.map(stack, getType);
    const altStackOps = _.map(altStack, getType);
    const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';
    const executionDescription = result === 'NoResult' ? `Executing ${stage.type}` : `Execution finished with result: ${result}`;

    console.log('findType', findElementType('ScriptConstant'));

    return (
      <div>
        <p><i>{executionDescription}</i></p>
        <p><b>Current Script:</b></p>
        <ScriptOpCodeList opCodes={currentScriptOps} />
        <p><b>Current Stack:</b></p>
        <ScriptOpCodeList opCodes={stackOps} />
        <p><b>Current Alt Stack:</b></p>
        <ScriptOpCodeList opCodes={altStackOps} />
        <p><b>ScriptPubKey:</b></p>
        <ScriptOpCodeList opCodes={scriptPubKeyOps} />
        <p><b>ScriptSig:</b></p>
        <ScriptOpCodeList opCodes={scriptSigOps} />

      </div>
    )
  }
};

const ScriptOpCodeList = ({opCodes}) => {
  return (
    <VirtualList
      className='ScriptOpCodeList'
      width='auto'
      height={100}
      scrollDirection='horizontal'
      overscanCount={10}
      itemCount={opCodes.length}
      itemSize={150} // Also supports variable heights (array or function getter)
      renderItem={({index, style}) => {
        const scriptElement = opCodes[index];
        const elementType = findElementType(scriptElement);
        const className = `OpCode ${elementType}`

        return (
          <div className={ className } key={index} style={style}>
            {opCodes[index]}
          </div>
        )

      }}
    />
  )
};

export default InterpreterComponent;