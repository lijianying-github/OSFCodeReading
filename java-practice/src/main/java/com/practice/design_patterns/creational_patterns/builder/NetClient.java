package com.practice.design_patterns.creational_patterns.builder;

/**
 * Description:Builder 建造者模式（核心参数必须传递，非核心可选构造复杂对象）
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/18
 */
public class NetClient {

    private String mUrl;

    private String mName;

    private int mVersion;

    private String mRequestMethod;

    private boolean isHasBody;

    public NetClient(String url, String requestMethod) {
        this.mUrl = url;
        this.mRequestMethod = requestMethod;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getName() {
        return mName;
    }

    public int getVersion() {
        return mVersion;
    }

    public String getRequestMethod() {
        return mRequestMethod;
    }

    public boolean isHasBody() {
        return isHasBody;
    }

    /**
     * 构建器builder
     */
    public static class Builder {

        private String mUrl;

        private String mName;

        private int mVersion;

        private String mRequestMethod;

        private boolean isHasBody;

        public NetClient build() {

            if (mUrl == null) {
                throw new RuntimeException("NetClient url is require==");
            }

            if (mRequestMethod == null) {
                throw new RuntimeException("NetClient requestMethod is require==");
            }

            NetClient client = new NetClient(mUrl, mRequestMethod);
            client.mName = mName;
            client.mVersion = mVersion;
            client.isHasBody = isHasBody;
            return client;
        }

        public Builder setUrl(String mUrl) {
            this.mUrl = mUrl;
            return this;
        }

        public Builder setName(String mName) {
            this.mName = mName;
            return this;
        }

        public Builder setVersion(int mVersion) {
            this.mVersion = mVersion;
            return this;
        }

        public Builder setRequestMethod(String mRequestMethod) {
            this.mRequestMethod = mRequestMethod;
            return this;
        }

        public Builder setHasBody(boolean hasBody) {
            isHasBody = hasBody;
            return this;
        }
    }

    @Override
    public String toString() {
        return "NetClient{" +
                "mUrl='" + mUrl + '\'' +
                ", mName='" + mName + '\'' +
                ", mVersion=" + mVersion +
                ", mRequestMethod='" + mRequestMethod + '\'' +
                ", isHasBody=" + isHasBody +
                '}';
    }
}
