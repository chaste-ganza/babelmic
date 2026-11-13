package com.babelmic.app.model;

/**
 * Holds the configuration for a single BabelMic translation session.
 *
 * <p>Created on the Language Selection screen and passed via Intent extras to
 * {@code DeviceDiscoveryActivity} and then to {@code ConversationActivity}.</p>
 */
public class SessionConfig {

    // Intent extra keys — used to pass this config between activities
    public static final String EXTRA_MY_LANGUAGE_TAG    = "extra_my_language_tag";
    public static final String EXTRA_MY_LANGUAGE_NAME   = "extra_my_language_name";
    public static final String EXTRA_IS_GROUP_OWNER     = "extra_is_group_owner";
    public static final String EXTRA_PEER_IP_ADDRESS    = "extra_peer_ip_address";

    private Language myLanguage;
    private boolean  isGroupOwner;
    private String   peerIpAddress; // set after Wi-Fi Direct group forms

    public SessionConfig(Language myLanguage) {
        this.myLanguage = myLanguage;
    }

    public Language getMyLanguage()          { return myLanguage;    }
    public void     setMyLanguage(Language l){ this.myLanguage = l;  }

    public boolean isGroupOwner()               { return isGroupOwner;    }
    public void    setGroupOwner(boolean owner) { this.isGroupOwner = owner; }

    public String getPeerIpAddress()             { return peerIpAddress;         }
    public void   setPeerIpAddress(String ip)    { this.peerIpAddress = ip;      }
}
