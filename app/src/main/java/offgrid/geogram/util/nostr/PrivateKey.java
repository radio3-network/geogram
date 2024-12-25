package offgrid.geogram.util.nostr;


/**
 *
 * @author squirrel
 */
public class PrivateKey extends BaseKey {

    public PrivateKey(byte[] rawData) {
        super(KeyType.PRIVATE, rawData, Bech32Prefix.NSEC);
    }

    public PrivateKey(String hexPrivKey) {
    	super(KeyType.PRIVATE, NostrUtil.hexToBytes(hexPrivKey), Bech32Prefix.NSEC);
    }
    
    /**
     * 
     * @return A strong pseudo random private key 
     */
    public static PrivateKey generateRandomPrivKey() {
    	return new PrivateKey(Schnorr.generatePrivateKey());
    }


}
