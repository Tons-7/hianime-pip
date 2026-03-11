public record Format(
        String url,
        int height,
        String label,
        String httpHeaders
) {
    @Override
    public String toString() {
        return label;
    }
}
