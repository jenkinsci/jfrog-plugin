package jenkins.plugins.jfrog.artifactoryclient.response;

public class ChecksumResponse {
    private Checksums checksums;

    public ChecksumResponse() {
    }

    public Checksums getChecksums() {
        return checksums;
    }

    public void setChecksums(Checksums checksums) {
        this.checksums = checksums;
    }

    public static class Checksums {
        private String sha1;
        private String md5;
        private String sha256;

        public String getSha256() { return sha256; }

        public void setSha256(String sha256) { this.sha256 = sha256; }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }

}
