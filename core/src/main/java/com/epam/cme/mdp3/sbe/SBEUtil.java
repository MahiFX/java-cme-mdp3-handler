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


@SuppressWarnings("unused")
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
            sb.append(msgName).append("[");
            SbeFieldType[] allFields = metadata.allFields();
            for (SbeFieldType sbeFieldType : allFields) {
                appendField(fields, sbeFieldType, sb);
            }

            SbeGroupType[] sbeGroupTypes = metadata.allGroups();
            if (sbeGroupTypes != null) {
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
        sb.append(name).append("(").append(id).append(")")
                .append("=");
        if (sbeFieldType.isString()) {
            String stringVal = stringField(sbeMessage, id, sbeFieldType.getLength());
            sb.append(stringVal);
        } else if (sbeFieldType.isFloat()) {
            double instance = doubleField(sbeMessage, id);
            sb.append(instance);
        } else if (sbeFieldType.getPrimitiveType() != null) {
            switch (sbeFieldType.getPrimitiveType()) {
                case Char:
                    sb.append(charField(sbeMessage, id));
                    break;
                case Int8:
                    sb.append(int8Field(sbeMessage, id));
                    break;
                case UInt8:
                    sb.append(uInt8Field(sbeMessage, id));
                    break;
                case Int16:
                    sb.append(int16Field(sbeMessage, id));
                    break;
                case UInt16:
                    sb.append(uInt16Field(sbeMessage, id));
                    break;
                case Int32:
                    sb.append(int32Field(sbeMessage, id));
                    break;
                case UInt32:
                    sb.append(uInt32Field(sbeMessage, id));
                    break;
                case Int64:
                    sb.append(int64Field(sbeMessage, id));
                    break;
                case UInt64:
                    sb.append(uInt64Field(sbeMessage, id));
                    break;
            }
        } else if (sbeFieldType.isComposite()) {
            SbeMonthYear monthYear = SbeMonthYear.instance();
            if (sbeMessage.getMonthYear(id, monthYear)) {
                sb.append(monthYear.getYear()).append("/").append(monthYear.getMonth())
                        .append("/").append(monthYear.getDay()).append(" (w").append(monthYear.getWeek()).append(")");
            } else {
                sb.append("<Unknown composite>");
            }
        } else {
            sb.append("<Unknown field type ").append(sbeFieldType.getFieldType().getName()).append(">");
        }
        sb.append(", ");
    }


    public static long int64Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getInt64(id);
        }
        return 0;
    }

    public static long uInt32Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getUInt32(id);
        }
        return 0;
    }

    public static int uInt16Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getUInt16(id);
        }
        return 0;
    }

    public static short int16Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getInt16(id);
        }
        return 0;
    }

    public static short uInt8Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getUInt8(id);
        }
        return 0;
    }

    public static byte int8Field(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getInt8(id);
        }
        return 0;
    }

    public static char charField(FieldSet sbeMessage, int id) {
        if (sbeMessage.hasField(id)) {
            return sbeMessage.getChar(id);
        }
        return '-';
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

    public static long uInt64Field(FieldSet fieldSet, int fieldId) {
        if (fieldSet.hasField(fieldId)) {
            return fieldSet.getUInt64(fieldId);
        }
        return 0;
    }

    public static int int32Field(FieldSet fieldSet, int fieldId) {
        if (fieldSet.hasField(fieldId)) {
            return fieldSet.getInt32(fieldId);
        }
        return 0;
    }
}
