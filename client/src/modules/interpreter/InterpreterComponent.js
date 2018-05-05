import _ from 'lodash';
import React from 'react';
import VirtualList from 'react-tiny-virtual-list';

const InterpreterContainer = ({interpretResult}) => {
  const {scriptPubKey, scriptSig, currentScript, stack, altStack, stage} = interpretResult.state;
  const getType = (op) => op.type;
  const formatScript = (array) => '[ ' + _.join(_.map(array, getType), ', ') + ' ]';

  const formattedPubKeyScript = formatScript(scriptPubKey);
  const formattedSigScript = formatScript(scriptSig);
  const formattedCurrentScript = formatScript(currentScript);
  const formattedStack = formatScript(stack);
  const formattedAltStack = formatScript(altStack);
  const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';
  const executionDescription = result === 'NoResult' ? `Executing ${stage.type}` : `Execution finished with result: ${result}`;

  const data = ['A', 'B', 'C', 'D', 'E', 'F'];

  return (
    <div>
      <p><i>{executionDescription}</i></p>
      <p><b>Current Script:</b></p>
      <p>{formattedCurrentScript}</p>
      <p><b>Current Stack:</b></p>
      <p>{formattedStack}</p>
      <p><b>Current Alt Stack:</b></p>
      <p>{formattedAltStack}</p>
      <p><b>ScriptPubKey:</b></p>
      <p>{formattedPubKeyScript}</p>
      <p><b>ScriptSig:</b></p>
      <p>{formattedSigScript}</p>


      <VirtualList
        width='100%'
        height={600}
        itemCount={data.length}
        itemSize={50} // Also supports variable heights (array or function getter)
        renderItem={({index, style}) =>
          <div key={index} style={style}> // The style property contains the item's absolute position
            Letter: {data[index]}, Row: #{index}
          </div>
        }
      />
    </div>
  )
};

export default InterpreterContainer;