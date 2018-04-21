package net.floodlightcontroller.core.internal;

import net.floodlightcontroller.debugevent.IDebugEventService;
import net.floodlightcontroller.debugevent.IDebugEventService.EventFieldType;
import org.projectfloodlight.openflow.types.DatapathId;

public class SwitchEvent {
    @IDebugEventService.EventColumn(name = "dpid", description = EventFieldType.DPID)
    DatapathId dpid;

    @IDebugEventService.EventColumn(name = "reason", description = EventFieldType.STRING)
    String reason;

    public SwitchEvent(DatapathId dpid, String reason) {
        this.dpid = dpid;
        this.reason = reason;
    }
}