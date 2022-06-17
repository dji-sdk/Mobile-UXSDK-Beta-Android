package dji.ux.beta.core.v4;

/**
 *
 */
public interface KeyIndexManager {

    @Deprecated
    void setKeyIndex(int keyIndex);

    int getKeyIndex();

    int getSubKeyIndex();

    void updateKeyOnIndex(int keyIndex ,int subKeyIndex);
}
