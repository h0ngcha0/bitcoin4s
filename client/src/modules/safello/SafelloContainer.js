import React from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';

class SafelloContainer extends React.Component {
  static propTypes = {};

  componentDidMount() {
    const script = document.createElement("script");
    script.async = true;
    script.src = "https://app.safello.com/sdk.js";
    this.div.appendChild(script);
  }

  componentDidUpdate(prevProps) {
  }

  render() {
    return (
      <div className="container" ref={el => (this.div = el)}>
        <div className={'application-definition'}>
          <a href={ `/`}>
            <img src={ mobileLogoImage } className={`logo-image img-responsive mobile`} alt="Bitcoin Playground"/>
          </a>
          <a href={ `/`}>
            <img src={ desktopLogoImage } className={`logo-image img-responsive desktop`} alt="Bitcoin Playground"/>
          </a>

          <div style={ {marginTop: '60px', textAlign: 'center'} }>
            <div className="safello-quickbuy" data-app-id="7a8569e1-0353-431c-be2e-eb1722b60ea6" data-border="true"
                 data-crypto="btc" data-utm-source="Softfork AB" />
          </div>
        </div>
      </div>
    );
  }
}

export default SafelloContainer;