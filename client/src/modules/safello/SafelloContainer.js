import React from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import {Grid, Typography, Divider} from '@material-ui/core';
class SafelloContainer extends React.Component {

  componentDidMount() {
    const script = document.createElement("script");
    script.async = true;
    script.src = "https://app.safello.com/sdk.js";
    this.div.appendChild(script);
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

          <div style={ {maxWidth: '480px', textAlign: 'left', margin: '0 auto'} }>
            <Grid container>
              <Grid item sm={12} xs={12} >
                <div style={ {paddingTop: '30px', paddingBottom: '8px', textAlign: 'left', paddingLeft: '4px'} }>
                  <Typography variant="h5" component="h4" color="textSecondary" gutterBottom>
                    Buy Bitcoin Instantly
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    If you are in Sweden, the easiest way to buy bitcoin is through Swish.
                    You can buy for up to 25000 SEK per week and bitcoin will be delivered to your address within minutes.
                  </Typography>
                </div>
              </Grid>
              <Grid item sm={12} xs={12} >
                <div className="safello-quickbuy" data-app-id="7a8569e1-0353-431c-be2e-eb1722b60ea6" data-border="true"
                     data-crypto="btc" data-utm-source="Softfork AB" />
              </Grid>
            </Grid>
          </div>
          <div style={ {textAlign: 'center'} }>
          </div>
        </div>
      </div>
    );
  }
}

export default SafelloContainer;