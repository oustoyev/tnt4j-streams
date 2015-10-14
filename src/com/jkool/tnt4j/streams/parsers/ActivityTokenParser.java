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

package com.jkool.tnt4j.streams.parsers;

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.*;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.log4j.Logger;

/**
 * <p>Implements an activity data parser that assumes each activity data item
 * is a token-separated string of fields, with the value for each field being
 * retrieved from a specific 1-based numeric token position.  The field-separator
 * can be customized.</p>
 * <p>This parser supports the following properties:
 * <ul>
 * <li>FieldDelim</li>
 * <li>Pattern</li>
 * <li>StripQuotes</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 5 $
 */
public class ActivityTokenParser extends ActivityParser
{
  private static final Logger logger = Logger.getLogger (ActivityTokenParser.class);

  /**
   * Contains the field separator (set by {@code FieldDelim} property) - Default: ","
   */
  protected StrMatcher fieldDelim = StrMatcher.charSetMatcher (",");

  /**
   * Indicates whether surrounding double quotes should be stripped from
   * extracted data values (set by {@code StripQuotes} property) - default: {@code true}
   */
  protected boolean stripQuotes = true;

  /**
   * Contains the pattern used to determine which types of activity data string this
   * parser supports (set by {@code Pattern} property).  When {@code null}, all
   * strings are assumed to match the format supported by this parser.
   */
  protected Pattern pattern = null;

  /**
   * Constructs an ActivityTokenParser.
   */
  public ActivityTokenParser ()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    { return; }
    super.setProperties (props);
    for (Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_FLD_DELIM.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        { fieldDelim = StrMatcher.charSetMatcher (value); }
        else
        { fieldDelim = null; }
        if (logger.isDebugEnabled ())
        { logger.debug ("Setting " + name + " to '" + fieldDelim + "'"); }
      }
      else if (StreamsConfig.PROP_PATTERN.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        {
          pattern = Pattern.compile (value);
          if (logger.isDebugEnabled ())
          { logger.debug ("Setting " + name + " to '" + value + "'"); }
        }
      }
      else if (StreamsConfig.PROP_STRIP_QUOTES.equalsIgnoreCase (name))
      {
        stripQuotes = Boolean.parseBoolean (value);
        if (logger.isTraceEnabled ())
        { logger.trace ("Setting " + name + " to '" + value + "'"); }
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p> This parser supports the following class types
   * (and all classes extending/implementing any of these):</p>
   * <ul>
   * <li>{@code java.lang.String}</li>
   * <li>{@code java.io.Reader}</li>
   * <li>{@code java.io.InputStream}</li>
   * </ul>
   */
  @Override
  public boolean isDataClassSupported (Object data)
  {
    return (
        String.class.isInstance (data) ||
        Reader.class.isInstance (data) ||
        InputStream.class.isInstance (data));
  }

  /**
   * Parses the specified raw activity data item, converting each field in raw data
   * to its corresponding value for passing to jKool Cloud Service.
   *
   * @param feeder parent feeder
   * @param data   raw activity data to parse
   *
   * @return converted activity info, or {@code null} if raw data string does not
   * match format for this parser
   *
   * @throws IllegalStateException if parser has not been properly initialized
   * @throws ParseException        if an error parsing raw data string
   */
  @Override
  public ActivityInfo parse (ActivityFeeder feeder, Object data) throws IllegalStateException, ParseException
  {
    if (fieldDelim == null)
    { throw new IllegalStateException ("ActivityTokenParser: field delimiter not specified or empty"); }
    if (data == null)
    { return null; }
    // Get next string to parse
    String dataStr = getNextString (data);
    if (StringUtils.isEmpty (dataStr))
    { return null; }
    if (logger.isDebugEnabled ())
    { logger.debug ("Parsing: " + dataStr); }
    if (pattern != null)
    {
      Matcher m = pattern.matcher (dataStr);
      if (m == null || !m.matches ())
      {
        if (logger.isDebugEnabled ())
        { logger.debug ("Input does not match pattern"); }
        return null;
      }
    }
    StrTokenizer tk;
    if (stripQuotes)
    { tk = new StrTokenizer (dataStr, fieldDelim, StrMatcher.doubleQuoteMatcher ()); }
    else
    { tk = new StrTokenizer (dataStr, fieldDelim); }
    tk.setIgnoreEmptyTokens (false);
    String[] fields = tk.getTokenArray ();
    if (fields == null || fields.length == 0)
    {
      if (logger.isDebugEnabled ())
      { logger.debug ("Did not find any fields in input string"); }
      return null;
    }
    if (logger.isDebugEnabled ())
    { logger.debug ("Split input into " + fields.length + " fields"); }
    ActivityInfo ai = new ActivityInfo ();
    ActivityField field = null;
    Object value = null;
    try
    {
      // save entire activity string as message data
      field = new ActivityField (ActivityFieldType.ActivityData);
      applyFieldValue (ai, field, dataStr);
      // apply fields for parser
      for (Map.Entry<ActivityField, ArrayList<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet ())
      {
        value = null;
        field = fieldEntry.getKey ();
        ArrayList<ActivityFieldLocator> locations = fieldEntry.getValue ();
        if (locations != null)
        {
          if (locations.size () == 1)
          {
            // field value is based on single raw data location, get the value of this location
            value = getLocatorValue (feeder, locations.get (0), fields);
          }
          else
          {
            // field value is based on contatenation of several raw data locations,
            // build array to hold data from each location
            Object[] values = new Object[locations.size ()];
            for (int l = 0; l < locations.size (); l++)
            { values[l] = getLocatorValue (feeder, locations.get (l), fields); }
            value = values;
          }
        }
        applyFieldValue (ai, field, value);
      }
    }
    catch (Exception e)
    {
      ParseException pe = new ParseException ("Failed parsing data for field " + field, 0);
      pe.initCause (e);
      throw pe;
    }
    return ai;
  }

  private Object getLocatorValue (ActivityFeeder feeder, ActivityFieldLocator locator, String[] fields) throws ParseException
  {
    Object val = null;
    if (locator != null)
    {
      String locStr = locator.getLocator ();
      if (!StringUtils.isEmpty (locStr))
      {
        if (locator.getBuiltInType () == ActivityFieldLocatorType.FeederProp)
        {
          val = feeder.getProperty (locStr);
        }
        else
        {
          int loc = Integer.parseInt (locStr);
          if (loc <= fields.length)
          { val = fields[loc - 1].trim (); }
        }
      }
      val = locator.formatValue (val);
    }
    return val;
  }
}