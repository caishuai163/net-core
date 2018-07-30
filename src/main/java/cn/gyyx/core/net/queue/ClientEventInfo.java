package cn.gyyx.core.net.queue;

import com.google.protobuf.GeneratedMessage;

import cn.gyyx.core.net.service.ProviderStrategryType;

/**
 * 定义客户端disrupter事件
 */
public class ClientEventInfo {

    private long id;

    private GeneratedMessage body;

    private String serviceName;

    private ProviderStrategryType strategryType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ProviderStrategryType getStrategryType() {
        return strategryType;
    }

    public void setStrategryType(ProviderStrategryType strategryType) {
        this.strategryType = strategryType;
    }

    public GeneratedMessage getBody() {
        return body;
    }

    public void setBody(GeneratedMessage body) {
        this.body = body;
    }

}
