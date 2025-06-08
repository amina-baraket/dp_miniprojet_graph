public enum DrawingTool {
    CIRCLE("Circle"),
    RECTANGLE("Rectangle"),
    SELECT("Select");
    
    private final String displayName;
    
    DrawingTool(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}