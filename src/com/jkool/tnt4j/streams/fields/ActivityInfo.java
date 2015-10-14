/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.fields;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.jkool.tnt4j.streams.types.LuwType;
import com.jkool.tnt4j.streams.types.ResourceManagerType;
import com.jkool.tnt4j.streams.types.ResourceType;
import com.jkool.tnt4j.streams.types.TransportType;
import com.jkool.tnt4j.streams.utils.*;
import com.nastel.jkool.tnt4j.core.*;
import com.nastel.jkool.tnt4j.tracker.Tracker;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class represents an activity (e.g. event or snapshot) to record with jKool Cloud Service.
 *
 * @version $Revision: 11 $
 */
public class ActivityInfo
{
  public static final String UNSPECIFIED_LABEL = "<UNSPECIFIED>";
  private static final String SNAPSHOT_CATEGORY = "TNTJ4-Streams"; //TODO: category name

  private static final Logger logger = Logger.getLogger (ActivityInfo.class);
  private static ConcurrentHashMap<String, String> hostCache = new ConcurrentHashMap<String, String> ();

  private long retryIntvl = 15000;

  private String serverName;      //??
  private String serverIp;                     //??
  private String osInfo = " ";    // so probe API does not fill in local information        //??
  private String applName;      //??
  private String userName;

  private String resourceMgr;
  private ResourceManagerType resourceMgrType;   //??   op.resource
  private String resource;            //??              op.resource
  private ResourceType resourceType;    //??                       op.resource

  private String actionName;
  private OpType actionType;
  private Timestamp startTime;
  private Timestamp endTime;
  private long elapsedTime = -1;
  private OpCompCode statusCode;
  private int reasonCode;
  private String errorMsg;
  private int severity = -1;
  private String location;
  private String correlator;

  private String msgSignature;
  private TransportType msgTransport;
  private String msgTag;
  private Object msgData;
  private String msgValue;

  /**
   * Constructs an ActivityInfo object.
   */
  public ActivityInfo ()
  {
  }

  /**
   * Applies the given value(s) for the specified field to the appropriate internal data
   * field for reporting field to the analyzer.
   *
   * @param field field to apply
   * @param value value to apply for this field, which could be an array of objects
   *              if value for field consists of multiple locations
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined format, etc.)
   */
  public void applyField (ActivityField field, Object value) throws ParseException
  {
    if (logger.isTraceEnabled ())
    { logger.trace ("Applying field " + field + "from: " + value); }
    ArrayList<ActivityFieldLocator> locators = field.getLocators ();
    Object fieldValue = null;
    if (value instanceof Object[])
    {
      Object[] values = (Object[]) value;
      if (values.length == 1)
      {
        value = values[0];
      }
    }
    if (value instanceof Object[])
    {
      Object[] values = (Object[]) value;
      if (field.isEnumeration ())
      { throw new ParseException ("Field " + field + ", multiple locators are not supported for enumeration-based fields", 0); }
      if (locators.size () != values.length)
      { throw new ParseException ("Failed parsing field: " + field + ", number of values does not match number of locators", 0); }
      StringBuilder sb = new StringBuilder ();
      for (int v = 0; v < values.length; v++)
      {
        ActivityFieldLocator locator = locators.get (v);
        String format = locator.getFormat ();
        Object fmtValue = formatValue (field, locator, values[v]);
        if (v > 0)
        { sb.append (field.getSeparator ()); }
        if (fmtValue != null)
        {
          if (fmtValue instanceof Timestamp && !StringUtils.isEmpty (format))
          { sb.append (((Timestamp) fmtValue).toString (format)); }
          else
          { sb.append (getStringValue (fmtValue)); }
        }
      }
      fieldValue = sb.toString ();
    }
    else
    {
      if (locators == null)
      { fieldValue = value; }
      else if (locators.size () > 1 && !(value instanceof Object[]))
      {
        fieldValue = value;    // field consists of multiple raw fields, but already been processed
      }
      else
      { fieldValue = formatValue (field, locators.get (0), value); }
    }
    if (fieldValue == null)
    {
      if (logger.isTraceEnabled ())
      { logger.trace ("field " + field + " resolves to null value, not applying field"); }
      return;
    }
    if (logger.isTraceEnabled ())
    { logger.trace ("Applying field " + field + ", value = " + fieldValue); }
    setFieldValue (field, fieldValue);
  }

  /**
   * Formats the value for the field based on the required internal data type of
   * the field and the definition of the field.
   *
   * @param field   field whose value is to be formatted
   * @param locator locator information for value
   * @param value   raw value of field
   *
   * @return formatted value of field in required internal data type
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined format, etc.)
   */
  protected Object formatValue (ActivityField field, ActivityFieldLocator locator, Object value) throws ParseException
  {
    if (value == null)
    { return null; }
    if (field.isEnumeration ())
    {
      if (value instanceof String)
      {
        if (StringUtils.containsOnly (value.toString (), "0123456789"))
        { value = Integer.parseInt (value.toString ()); }
        else
        { value = ((String) value).toUpperCase ().trim (); }
      }
    }
    switch (field.getFieldType ())
    {
      case ElapsedTime:
        try
        {
          // Elapsed time needs to be converted to usec
          ActivityFieldUnitsType units = ActivityFieldUnitsType.valueOf (locator.getUnits ());
          if (!(value instanceof Number))
          { value = Long.parseLong (value.toString ()); }
          value = TimestampFormatter.convert ((Number) value, units, ActivityFieldUnitsType.Microseconds);
        }
        catch (Exception e) {}
        break;
      case Resource:
//        if (value instanceof Resource)
//        { value = ((Resource) value).getName (); }
        value = String.valueOf (value);
        break;
      case ServerIp:
        if (value instanceof InetAddress)
        { value = ((InetAddress) value).getHostAddress (); }
        break;
      case ServerName:
        if (value instanceof InetAddress)
        { value = ((InetAddress) value).getHostName (); }
        break;
      default:
        break;
    }
    return value;
  }

  /**
   * Sets field to specified value, handling any necessary conversions
   * based on internal data type for field.
   *
   * @param field      field whose value is to be set
   * @param fieldValue formatted value based on locator definition for field
   *
   * @throws ParseException if there are any errors with conversion to internal format
   */
  private void setFieldValue (ActivityField field, Object fieldValue) throws ParseException
  {
    switch (field.getFieldType ())
    {
      case ActivityData:
        msgData = fieldValue;
        break;
      case ActivityName:
        actionName = getStringValue (fieldValue);
        break;
      case ActivityType:
        actionType = Utils.mapOpType (fieldValue);
        break;
      case ApplName:
        applName = getStringValue (fieldValue);
        break;
      case Correlator:
        correlator = getStringValue (fieldValue);
        break;
      case ElapsedTime:
        if (fieldValue instanceof Number)
        { elapsedTime = ((Number) fieldValue).longValue (); }
        else
        { elapsedTime = Long.parseLong (getStringValue (fieldValue)); }
        break;
      case EndTime:
        if (fieldValue instanceof Timestamp)
        { endTime = (Timestamp) fieldValue; }
        else
        { endTime = TimestampFormatter.parse (field.getFormat (), fieldValue, null, field.getLocale ()); }
        break;
      case ErrorMsg:
        errorMsg = getStringValue (fieldValue);
        break;
      case Location:
        location = getStringValue (fieldValue);
        break;
      case ReasonCode:
        if (fieldValue instanceof Number)
        { reasonCode = ((Number) fieldValue).intValue (); }
        else
        { reasonCode = Integer.parseInt (getStringValue (fieldValue)); }
        break;
      case ResMgrType:
        resourceMgrType = ResourceManagerType.valueOf (fieldValue);
        break;
      case Resource:
        resource = getStringValue (fieldValue);
        break;
      case ResourceMgr:
        resourceMgr = getStringValue (fieldValue);
        break;
      case ResType:
        resourceType = ResourceType.valueOf (fieldValue);
        break;
      case ServerIp:
        serverIp = getStringValue (fieldValue);
        break;
      case ServerName:
        serverName = getStringValue (fieldValue);
        break;
      case ServerOs:
        osInfo = getStringValue (fieldValue);
        break;
      case Severity:
        if (fieldValue instanceof Number)
        {
          severity = ((Number) fieldValue).intValue ();
        }
        else
        {
          try
          {
            severity = OpLevel.valueOf (fieldValue).ordinal ();
          }
          catch (Exception e)
          {
            severity = Integer.parseInt (getStringValue (fieldValue));
          }
        }
        break;
      case Signature:
        msgSignature = getStringValue (fieldValue);
        break;
      case StartTime:
        if (fieldValue instanceof Timestamp)
        { startTime = (Timestamp) fieldValue; }
        else
        { startTime = TimestampFormatter.parse (field.getFormat (), fieldValue, null, field.getLocale ()); }
        break;
      case StatusCode:
        statusCode = OpCompCode.valueOf (fieldValue);
        break;
      case Tag:
        msgTag = getStringValue (fieldValue);
        break;
      case Transport:
        msgTransport = TransportType.valueOf (fieldValue);
        break;
      case UserName:
        userName = getStringValue (fieldValue);
        break;
      case Value:
        msgValue = getStringValue (fieldValue);
        break;
      default:
        throw new IllegalArgumentException ("Unrecognized Activity field: " + field);
    }
    if (logger.isTraceEnabled ())
    { logger.trace ("Set field " + field + " to '" + fieldValue + "'"); }
  }

  /**
   * Creates the appropriate data message to send to jKool Cloud Service and
   * records the activity with the analyzer using the specified communications definition.
   *
   * @param tracker communication gateway to use to record activity
   *
   * @throws Throwable indicates an error building data message or sending data
   *                   to analyzer
   */
  public void recordActivity (Tracker tracker) throws Throwable
  {
    if (tracker == null)
    {
      logger.warn ("Activity destination not specified, activity not being recorded");
      return;
    }
    resolveServer ();
    determineTimes ();
    String signature = StringUtils.isEmpty (msgSignature) ? UUID.randomUUID ().toString () : msgSignature;
    String correl = StringUtils.isEmpty (correlator) ? signature : correlator;
    TrackingEvent event =
        tracker.newEvent (severity < 0 ? OpLevel.INFO : OpLevel.valueOf (severity), actionName, correl, "", (Object[]) null);
    event.setTrackingId (signature);
    event.setTag (msgTag);
    if (msgData != null)
    {
      if (msgData instanceof byte[])
      {
        byte[] binData = (byte[]) msgData;
        event.setMessage (binData, (Object[]) null);
        event.setSize (binData.length);
      }
      else
      {
        event.setEncoding (com.nastel.jkool.tnt4j.core.Message.ENCODING_NONE);
        String strData = String.valueOf (msgData);
        event.setMessage (strData, (Object[]) null);
        event.setSize (strData.length ());
      }
    }
    event.getOperation ().setCompCode (statusCode == null ? OpCompCode.SUCCESS : statusCode);
    event.getOperation ().setReasonCode (reasonCode);
    event.getOperation ().setType (actionType);
    event.getOperation ().setException (errorMsg);
    event.getOperation ().setLocation (location);
    event.getOperation ().setResource (StringUtils.isEmpty (resourceMgr) ? UNSPECIFIED_LABEL : resourceMgr);
    event.getOperation ().setUser (userName);
    event.getOperation ().setTID (Thread.currentThread ().getId ());
    event.getOperation ().setSeverity (severity < 0 ? OpLevel.INFO : OpLevel.valueOf (severity));
    event.start (startTime);
    event.stop (endTime, elapsedTime);
    Snapshot snapshot = tracker.newSnapshot (SNAPSHOT_CATEGORY, event.getOperation ().getName ());
    //snapshot.setParentId (event);
    snapshot.add (Constants.XML_APPL_NAME_LABEL, applName);
    snapshot.add (Constants.XML_SERVER_NAME_LABEL, serverName);
    snapshot.add (Constants.XML_SERVER_IP_LABEL, serverIp);
    snapshot.add (Constants.XML_SERVER_CPU_COUNT_LABEL, 1);
    snapshot.add (Constants.XML_SERVER_OS_LABEL, osInfo);
    snapshot.add (Constants.XML_APPL_USER_LABEL, userName);
    snapshot.add (Constants.XML_RESMGR_NAME_LABEL, StringUtils.isEmpty (resourceMgr) ? UNSPECIFIED_LABEL : resourceMgr);
    snapshot.add (Constants.XML_RESMGR_TYPE_LABEL, resourceMgrType == null ? ResourceManagerType.UNKNOWN : resourceMgrType);
    snapshot.add (Constants.XML_RESMGR_SERVER_LABEL, serverName);
    snapshot.add (Constants.XML_LUW_SIGNATURE_LABEL, UUID.randomUUID ().toString ());
    snapshot.add (Constants.XML_LUW_TID_LABEL, Thread.currentThread ().getId ());
    snapshot.add (Constants.XML_LUW_START_TIME_SEC_LABEL, startTime);
    snapshot.add (Constants.XML_LUW_END_TIME_SEC_LABEL, endTime);
    snapshot.add (Constants.XML_LUW_STATUS_LABEL, statusCode == OpCompCode.ERROR ? ActivityStatus.EXCEPTION : ActivityStatus.END);
    snapshot.add (Constants.XML_LUW_TYPE_LABEL,
                  actionType == OpType.RECEIVE ? LuwType.CONSUMER : actionType == OpType.SEND ? LuwType.PRODUCER : null);
    snapshot.add (Constants.XML_OP_FUNC_LABEL, actionName);
    snapshot.add (Constants.XML_OP_TYPE_LABEL, actionType == null ? OpType.OTHER : actionType);
    snapshot.add (Constants.XML_OP_USER_NAME_LABEL, userName);
    snapshot.add (Constants.XML_OP_CC_LABEL, statusCode == null ? OpCompCode.SUCCESS : statusCode);
    snapshot.add (Constants.XML_OP_RC_LABEL, reasonCode);
    snapshot.add (Constants.XML_OP_EXCEPTION_LABEL, errorMsg);
    snapshot.add (Constants.XML_OP_START_TIME_SEC_LABEL, startTime);
    snapshot.add (Constants.XML_OP_END_TIME_SEC_LABEL, endTime);
    snapshot.add (Constants.XML_OP_ELAPSED_TIME_LABEL, elapsedTime);
    snapshot.add (Constants.XML_OP_SEVERITY_LABEL, severity < 0 ? OpLevel.INFO : OpLevel.valueOf (severity));
    snapshot.add (Constants.XML_OP_LOCATION_LABEL, location);
    snapshot.add (Constants.XML_OP_CORRELATOR_LABEL, correlator);
    snapshot.add (Constants.XML_OP_RES_NAME_LABEL, StringUtils.isEmpty (resource) ? UNSPECIFIED_LABEL : resource);
    snapshot.add (Constants.XML_OP_RES_TYPE_LABEL, resourceType == null ? ResourceType.UNKNOWN : resourceType);
    snapshot.add (Constants.XML_MSG_SIGNATURE_LABEL, signature);
    snapshot.add (Constants.XML_MSG_TRANSPORT_LABEL, msgTransport == null ? TransportType.UNKNOWN : msgTransport);
    snapshot.add (Constants.XML_MSG_TAG_LABEL, msgTag);
    snapshot.add (Constants.XML_MSG_CORRELATOR_LABEL, correlator);
    snapshot.add (Constants.XML_MSG_VALUE_LABEL, msgValue);
    if (msgData != null)
    {
      if (msgData instanceof byte[])
      {
        byte[] binData = (byte[]) msgData;
        snapshot.add (Constants.XML_NAS_MSG_BINDATA_LABEL, binData);
        snapshot.add (Constants.XML_MSG_SIZE_LABEL, binData.length);
      }
      else
      {
        String strData = String.valueOf (msgData);
        snapshot.add (Constants.XML_NAS_MSG_STRDATA_LABEL, strData);
        snapshot.add (Constants.XML_MSG_SIZE_LABEL, strData.length ());
      }
    }
    event.getOperation ().addSnapshot (snapshot);
//    TrackingActivity activity = tracker.newActivity (severity < 0 ? OpLevel.INFO : OpLevel.valueOf (severity), actionName, signature);
//    activity.start ();
//    //activity.setCorrelator (correlator);
//    activity.setUser (userName);
//    activity.tnt (event);
    StreamsThread thread = null;
    if (Thread.currentThread () instanceof StreamsThread)
    { thread = (StreamsThread) Thread.currentThread (); }
    boolean retryAttempt = false;
    do
    {
      if (event != null)    //if (activity != null)
      {
        try
        {
          tracker.tnt (event);
//          activity.stop ();
//          tracker.tnt (activity);
          if (retryAttempt)
          { logger.info ("Activity recording retry successful"); }
          return;
        }
        catch (Throwable ioe)
        {
          logger.error ("Failed recording activity", ioe);
          tracker.close ();
          if (thread == null)
          { throw ioe; }
          retryAttempt = true;
          logger.info ("Will retry recording in " + retryIntvl / 1000 + " seconds");
          StreamsThread.sleep (retryIntvl);
        }
      }
    }
    while (thread != null && !thread.isStopRunning ());
  }

  /**
   * Resolves server name and/or IP Address based on values specified.
   */
  private void resolveServer ()
  {
    if (StringUtils.isEmpty (serverName) && StringUtils.isEmpty (serverIp))
    {
      serverName = Utils.getLocalHostName ();
      serverIp = Utils.getLocalHostAddress ();
      osInfo = null;    // probe API will then fill in local information
    }
    else if (StringUtils.isEmpty (serverName))
    {
      if (StringUtils.isEmpty (serverIp))
      {
        serverName = Utils.getLocalHostName ();
        serverIp = Utils.getLocalHostAddress ();
        osInfo = null;    // probe API will then fill in local information
      }
      else
      {
        try
        {
          serverName = hostCache.get (serverIp);
          if (StringUtils.isEmpty (serverName))
          {
            serverName = Utils.resolveAddressToHostName (serverIp);
            if (StringUtils.isEmpty (serverName))
            {
              // Add entry so we don't repeatedly attempt to look up unresolvable IP Address
              hostCache.put (serverIp, "");
            }
            else
            {
              hostCache.put (serverIp, serverName);
              hostCache.put (serverName, serverIp);
            }
          }
        }
        catch (Exception e)
        {
          serverName = serverIp;
        }
      }
    }
    else if (StringUtils.isEmpty (serverIp))
    {
      serverIp = hostCache.get (serverName);
      if (StringUtils.isEmpty (serverIp))
      {
        serverIp = Utils.resolveHostNameToAddress (serverName);
        if (StringUtils.isEmpty (serverIp))
        {
          // Add entry so we don't repeatedly attempt to look up unresolvable host name
          hostCache.put (serverName, "");
        }
        else
        {
          hostCache.put (serverIp, serverName);
          hostCache.put (serverName, serverIp);
        }
      }
    }
    if (StringUtils.isEmpty (serverIp))
    {
      serverIp = " "; // prevents probe API from resolving it to the local IP address
    }
  }

  /**
   * Computes the unspecified operation times and/or elapsed time based
   * on the specified ones.
   */
  private void determineTimes ()
  {
    if (elapsedTime < 0)
    { elapsedTime = 0; }
    if (endTime == null)
    {
      if (startTime != null)
      {
        endTime = new Timestamp (startTime);
        endTime.add (0, elapsedTime);
      }
      else
      {
        endTime = new Timestamp ();
      }
    }
    if (startTime == null)
    {
      startTime = new Timestamp (endTime);
      startTime.subtract (0, elapsedTime);
    }
  }

  /**
   * Returns the appropriate string representation for the specified value.
   *
   * @param value value to convert to string representation
   *
   * @return string representation of value
   */
  private String getStringValue (Object value)
  {
    if (value instanceof String)
    { return ((String) value); }
    if (value instanceof byte[])
    { return (new String ((byte[]) value)); }
    return value.toString ();
  }

  public long getRetryIntvl ()
  {
    return retryIntvl;
  }

  public String getServerName ()
  {
    return serverName;
  }

  public String getServerIp ()
  {
    return serverIp;
  }

  public String getOsInfo ()
  {
    return osInfo;
  }

  public String getApplName ()
  {
    return applName;
  }

  public String getUserName ()
  {
    return userName;
  }

  public String getResourceMgr ()
  {
    return resourceMgr;
  }

  public ResourceManagerType getResourceMgrType ()
  {
    return resourceMgrType;
  }

  public String getResource ()
  {
    return resource;
  }

  public ResourceType getResourceType ()
  {
    return resourceType;
  }

  public String getActionName ()
  {
    return actionName;
  }

  public OpType getActionType ()
  {
    return actionType;
  }

  public Timestamp getStartTime ()
  {
    return startTime;
  }

  public Timestamp getEndTime ()
  {
    return endTime;
  }

  public long getElapsedTime ()
  {
    return elapsedTime;
  }

  public OpCompCode getStatusCode ()
  {
    return statusCode;
  }

  public int getReasonCode ()
  {
    return reasonCode;
  }

  public String getErrorMsg ()
  {
    return errorMsg;
  }

  public int getSeverity ()
  {
    return severity;
  }

  public String getLocation ()
  {
    return location;
  }

  public String getMsgSignature ()
  {
    return msgSignature;
  }

  public TransportType getMsgTransport ()
  {
    return msgTransport;
  }

  public String getMsgTag ()
  {
    return msgTag;
  }

  public String getCorrelator ()
  {
    return correlator;
  }

  public Object getMsgData ()
  {
    return msgData;
  }

  public String getMsgValue ()
  {
    return msgValue;
  }
}