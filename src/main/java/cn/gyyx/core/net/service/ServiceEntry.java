package cn.gyyx.core.net.service;

/**
 * 业务实体</br>
 * 包括属性：
 * <ul>
 * <li>{@link #serviceName} 业务名称</li>
 * <li>{@link #ip} ip</li>
 * <li>{@link #port} 端口</li>
 * </ul>
 * 
 */
public class ServiceEntry {

    private String serviceName;

    private String ip;

    private int port;

    public ServiceEntry() {

    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String address() {
        return ip + ":" + port;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        if (obj instanceof ServiceEntry) {

            ServiceEntry entry = (ServiceEntry) obj;
            if (this.ip.equals(entry.getIp()) && this.port == entry.getPort()
                    && this.serviceName.equals(entry.getServiceName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {

        return (this.getServiceName() + ";" + this.ip + ":" + this.port)
                .hashCode();
    }
}
