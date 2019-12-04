/*
 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.elasticfilesystem.model;

import java.io.Serializable;
import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 */
public class DescribeMountTargetsRequest extends AmazonWebServiceRequest
        implements Serializable, Cloneable {

    /**
     * <p>
     * Optional. Maximum number of mount targets to return in the response. It
     * must be an integer with a value greater than zero.
     * </p>
     */
    private Integer maxItems;
    /**
     * <p>
     * Optional. String. Opaque pagination token returned from a previous
     * <code>DescribeMountTargets</code> operation. If present, it specifies to
     * continue the list from where the previous returning call left off.
     * </p>
     */
    private String marker;
    /**
     * <p>
     * String. The ID of the file system whose mount targets you want to list.
     * </p>
     */
    private String fileSystemId;

    /**
     * <p>
     * Optional. Maximum number of mount targets to return in the response. It
     * must be an integer with a value greater than zero.
     * </p>
     * 
     * @param maxItems
     *        Optional. Maximum number of mount targets to return in the
     *        response. It must be an integer with a value greater than zero.
     */
    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    /**
     * <p>
     * Optional. Maximum number of mount targets to return in the response. It
     * must be an integer with a value greater than zero.
     * </p>
     * 
     * @return Optional. Maximum number of mount targets to return in the
     *         response. It must be an integer with a value greater than zero.
     */
    public Integer getMaxItems() {
        return this.maxItems;
    }

    /**
     * <p>
     * Optional. Maximum number of mount targets to return in the response. It
     * must be an integer with a value greater than zero.
     * </p>
     * 
     * @param maxItems
     *        Optional. Maximum number of mount targets to return in the
     *        response. It must be an integer with a value greater than zero.
     * @return Returns a reference to this object so that method calls can be
     *         chained together.
     */
    public DescribeMountTargetsRequest withMaxItems(Integer maxItems) {
        setMaxItems(maxItems);
        return this;
    }

    /**
     * <p>
     * Optional. String. Opaque pagination token returned from a previous
     * <code>DescribeMountTargets</code> operation. If present, it specifies to
     * continue the list from where the previous returning call left off.
     * </p>
     * 
     * @param marker
     *        Optional. String. Opaque pagination token returned from a previous
     *        <code>DescribeMountTargets</code> operation. If present, it
     *        specifies to continue the list from where the previous returning
     *        call left off.
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * <p>
     * Optional. String. Opaque pagination token returned from a previous
     * <code>DescribeMountTargets</code> operation. If present, it specifies to
     * continue the list from where the previous returning call left off.
     * </p>
     * 
     * @return Optional. String. Opaque pagination token returned from a
     *         previous <code>DescribeMountTargets</code> operation. If present,
     *         it specifies to continue the list from where the previous
     *         returning call left off.
     */
    public String getMarker() {
        return this.marker;
    }

    /**
     * <p>
     * Optional. String. Opaque pagination token returned from a previous
     * <code>DescribeMountTargets</code> operation. If present, it specifies to
     * continue the list from where the previous returning call left off.
     * </p>
     * 
     * @param marker
     *        Optional. String. Opaque pagination token returned from a previous
     *        <code>DescribeMountTargets</code> operation. If present, it
     *        specifies to continue the list from where the previous returning
     *        call left off.
     * @return Returns a reference to this object so that method calls can be
     *         chained together.
     */
    public DescribeMountTargetsRequest withMarker(String marker) {
        setMarker(marker);
        return this;
    }

    /**
     * <p>
     * String. The ID of the file system whose mount targets you want to list.
     * </p>
     * 
     * @param fileSystemId
     *        String. The ID of the file system whose mount targets you want to
     *        list.
     */
    public void setFileSystemId(String fileSystemId) {
        this.fileSystemId = fileSystemId;
    }

    /**
     * <p>
     * String. The ID of the file system whose mount targets you want to list.
     * </p>
     * 
     * @return String. The ID of the file system whose mount targets you want to
     *         list.
     */
    public String getFileSystemId() {
        return this.fileSystemId;
    }

    /**
     * <p>
     * String. The ID of the file system whose mount targets you want to list.
     * </p>
     * 
     * @param fileSystemId
     *        String. The ID of the file system whose mount targets you want to
     *        list.
     * @return Returns a reference to this object so that method calls can be
     *         chained together.
     */
    public DescribeMountTargetsRequest withFileSystemId(String fileSystemId) {
        setFileSystemId(fileSystemId);
        return this;
    }

    /**
     * Returns a string representation of this object; useful for testing and
     * debugging.
     *
     * @return A string representation of this object.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getMaxItems() != null)
            sb.append("MaxItems: " + getMaxItems() + ",");
        if (getMarker() != null)
            sb.append("Marker: " + getMarker() + ",");
        if (getFileSystemId() != null)
            sb.append("FileSystemId: " + getFileSystemId());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof DescribeMountTargetsRequest == false)
            return false;
        DescribeMountTargetsRequest other = (DescribeMountTargetsRequest) obj;
        if (other.getMaxItems() == null ^ this.getMaxItems() == null)
            return false;
        if (other.getMaxItems() != null
                && other.getMaxItems().equals(this.getMaxItems()) == false)
            return false;
        if (other.getMarker() == null ^ this.getMarker() == null)
            return false;
        if (other.getMarker() != null
                && other.getMarker().equals(this.getMarker()) == false)
            return false;
        if (other.getFileSystemId() == null ^ this.getFileSystemId() == null)
            return false;
        if (other.getFileSystemId() != null
                && other.getFileSystemId().equals(this.getFileSystemId()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode
                + ((getMaxItems() == null) ? 0 : getMaxItems().hashCode());
        hashCode = prime * hashCode
                + ((getMarker() == null) ? 0 : getMarker().hashCode());
        hashCode = prime
                * hashCode
                + ((getFileSystemId() == null) ? 0 : getFileSystemId()
                        .hashCode());
        return hashCode;
    }

    @Override
    public DescribeMountTargetsRequest clone() {
        return (DescribeMountTargetsRequest) super.clone();
    }
}