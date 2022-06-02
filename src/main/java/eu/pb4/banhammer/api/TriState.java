package eu.pb4.banhammer.api;


public enum TriState {
    FALSE,
    DEFAULT,
    TRUE;

    public boolean get() {
        return this == TRUE;
    }

    public boolean get(boolean defaultValue) {
        return this == DEFAULT ? defaultValue : this == TRUE;
    }
}
