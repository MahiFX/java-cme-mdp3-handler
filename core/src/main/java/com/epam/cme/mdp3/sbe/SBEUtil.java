package com.epam.cme.mdp3.sbe;

import com.epam.cme.mdp3.FieldSet;
import com.epam.cme.mdp3.MdpGroup;
import com.epam.cme.mdp3.MdpGroupEntry;
import com.epam.cme.mdp3.MdpMessage;
import com.epam.cme.mdp3.sbe.message.*;
import com.epam.cme.mdp3.sbe.message.meta.MetadataContainer;
import com.epam.cme.mdp3.sbe.message.meta.SbeFieldType;
import com.epam.cme.mdp3.sbe.message.meta.SbeGroupType;
import com.epam.cme.mdp3.sbe.message.meta.SbePrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SBEUtil {

    private static final Logger logger = LoggerFactory.getLogger(SBEUtil.class);

    private SBEUtil() {
    }

    public static String dumpAllFields(MdpMessage mdpMessage) {
        try {

            if (mdpMessage instanceof SbeMessage) {
                SbeMessage sbeMessage = (SbeMessage) mdpMessage;
                String msgName = sbeMessage.getMessageType().getMessageType().getName();
                return dumpAllFields(sbeMessage, msgName);
            }

            return "Unknown message type: " + mdpMessage.getClass().getName();

        } catch (Throwable t) {
            logger.error("Error dumping fields for message: {}", mdpMessage, t);
            return "Error dumping fields: " + t.getMessage();
        }
    }

    public static String dumpAllFields(AbstractFieldSet fields, String msgName) {
        try {
            StringBuilder sb = new StringBuilder();
            MetadataContainer metadata = fields.metadata();
            sb.append(msgName)
                    .append("[");
            SbeFieldType[] allFields = metadata.allFields();
            for (SbeFieldType sbeFieldType : allFields) {
                appendField(fields, sbeFieldType, sb);
            }

            SbeGroupType[] sbeGroupTypes = metadata.allGroups();
            for (SbeGroupType sbeGroupType : sbeGroupTypes) {
                MetadataContainer groupMeta = sbeGroupType.getMetadataContainer();
                MdpGroup mdpGroup = SbeGroup.instance();
                if (fields.getGroup(sbeGroupType.getGroupType().getId(), mdpGroup)) {
                    sb.append(sbeGroupType.getGroupType().getName()).append("(count ").append(mdpGroup.getNumInGroup()).append(")=[");
                    SbeFieldType[] groupFields = groupMeta.allFields();
                    while (mdpGroup.hashNext()) {
                        mdpGroup.next();
                        MdpGroupEntry event = SbeGroupEntry.instance();
                        mdpGroup.getEntry(event);
                        for (SbeFieldType sbeFieldType : groupFields) {
                            appendField(event, sbeFieldType, sb);
                        }
                    }
                    sb.append("]");
                }
            }

            return sb.toString();
        } catch (Throwable t) {
            logger.error("Error dumping fields for message: {}", msgName, t);
            return "Error dumping fields: " + t.getMessage();
        }
    }

    private static void appendField(FieldSet sbeMessage, SbeFieldType sbeFieldType, StringBuilder sb) {
        if (sbeFieldType == null || sbeFieldType.getFieldType() == null) {
            return;
        }
        int id = sbeFieldType.getFieldType().getId();
        if (!sbeMessage.hasField(id)) return;

        String name = sbeFieldType.getFieldType().getName();
        if (sbeFieldType.isString()) {
            String stringVal = stringField(sbeMessage, id, sbeFieldType.getLength());
            sb.append(name).append("=").append(stringVal).append(", ");
        } else if (sbeFieldType.isFloat()) {
            double instance = doubleField(sbeMessage, id);
            sb.append(name).append("=").append(instance).append(", ");
        } else if (sbeFieldType.getPrimitiveType() != null) {
            switch (sbeFieldType.getPrimitiveType()) {
                case Char:
                    SbeString charVal = SbeString.allocate(1);
                    sbeMessage.getString(id, charVal);
                    sb.append(name).append("=").append(charVal.getString()).append(", ");
                    break;
                case Int8:
                    int int8Val = sbeMessage.getInt8(id);
                    sb.append(name).append("=").append(int8Val).append(", ");
                    break;

                case UInt8:
                    int uint8Val = sbeMessage.getUInt8(id);
                    sb.append(name).append("=").append(uint8Val).append(", ");
                    break;
                case Int16:
                    int int16Val = sbeMessage.getInt16(id);
                    sb.append(name).append("=").append(int16Val).append(", ");
                    break;
                case UInt16:
                    int uint16Val = sbeMessage.getUInt16(id);
                    sb.append(name).append("=").append(uint16Val).append(", ");
                    break;
                case Int32:
                    int int32Val = sbeMessage.getInt32(id);
                    sb.append(name).append("=").append(int32Val).append(", ");
                    break;
                case UInt32:
                    long uint32Val = sbeMessage.getUInt32(id);
                    sb.append(name).append("=").append(uint32Val).append(", ");
                    break;
                case Int64:
                    long int64Val = sbeMessage.getInt64(id);
                    sb.append(name).append("=").append(int64Val).append(", ");
                    break;
                case UInt64:
                    long uint64Val = sbeMessage.getUInt64(id);
                    sb.append(name).append("=").append(uint64Val).append(", ");
                    break;
            }
        } else if (sbeFieldType.isComposite()) {
            SbeMonthYear monthYear = SbeMonthYear.instance();
            if (sbeMessage.getMonthYear(id, monthYear)) {
                sb.append(name).append("=")
                        .append(monthYear.getYear()).append("/").append(monthYear.getMonth())
                        .append("/").append(monthYear.getDay()).append(" (w").append(monthYear.getWeek()).append(")");
            } else {
                sb.append(name).append("=<Unknown composite>, ");
            }
        } else {
            sb.append(name).append("=<Unknown field type ").append(sbeFieldType.getFieldType().getName()).append(">, ");
        }
    }


    public static String stringField(FieldSet mdpMessage, int fieldId, int length) {
        if (!mdpMessage.hasField(fieldId)) {
            return null;
        }
        SbeString asset = SbeString.allocate(length);
        try {
            mdpMessage.getString(fieldId, asset);
            return asset.toString();
        } catch (Throwable t) {
            logger.trace("Failed to get string field {} of length {}: {}", fieldId, length, t.getMessage());
            return "unknown";
        }
    }

    public static double doubleField(FieldSet mdpMessage, int fieldId) {
        if (!mdpMessage.hasField(fieldId)) {
            return Double.NaN;
        }
        SbeDouble dbl = SbeDouble.instance();
        try {
            mdpMessage.getDouble(fieldId, dbl);
            if (dbl.isNull() || dbl.getMantissa() == SbePrimitiveType.UInt64.getNullValue()) {
                return Double.NaN;
            }
            return dbl.asDouble();
        } catch (Throwable t) {
            logger.trace("Failed to get double field {}: {}", fieldId, t.getMessage());
            return Double.NaN;
        }
    }
}
