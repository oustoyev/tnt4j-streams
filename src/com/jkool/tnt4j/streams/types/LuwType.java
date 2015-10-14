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

package com.jkool.tnt4j.streams.types;

/**
 * Provides list of valid Logical Unit of Work (LUW) types.
 *
 * @version $Revision: 7 $
 */
public enum LuwType
{
  UNKNOWN, PRODUCER, PROCESSOR, CONSUMER;

  private static LuwType[] enumList = LuwType.values ();

  /**
   * Converts the specified value to a member of the enumeration.
   *
   * @param value enumeration value to convert
   *
   * @return enumeration member
   *
   * @throws IllegalArgumentException if there is no
   *                                  member of the enumeration with the specified value
   */
  public static LuwType valueOf (int value)
  {
    if (value < 0 || value >= enumList.length)
    { throw new IllegalArgumentException ("value '" + value + "' is not valid for enumeration LuwType"); }
    return enumList[value];
  }

  /**
   * Converts the specified object to a member of the enumeration.
   *
   * @param value object to convert
   *
   * @return enumeration member
   *
   * @throws NullPointerException     if value is <code>null</code>
   * @throws IllegalArgumentException if object cannot be matched to a
   *                                  member of the enumeration
   */
  public static LuwType valueOf (Object value)
  {
    if (value == null)
    { throw new NullPointerException ("object must be non-null"); }
    if (value instanceof Number)
    { return valueOf (((Number) value).intValue ()); }
    else if (value instanceof String)
    { return valueOf (value.toString ()); }
    throw new IllegalArgumentException ("Cannot convert object of type '" + value.getClass ().getName () + "' enum LuwType");
  }
}