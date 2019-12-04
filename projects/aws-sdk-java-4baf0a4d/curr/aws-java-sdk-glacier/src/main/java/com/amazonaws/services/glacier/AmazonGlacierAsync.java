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
package com.amazonaws.services.glacier;

import java.util.concurrent.Future;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.glacier.model.*;

/**
 * Interface for accessing AmazonGlacier asynchronously.
 * Each asynchronous method will return a Java Future object, and users are also allowed
 * to provide a callback handler.
 * <p>
 * Amazon Glacier is a storage solution for "cold data."
 * </p>
 * <p>
 * Amazon Glacier is an extremely low-cost storage service that provides
 * secure, durable, and easy-to-use storage for data backup and archival.
 * With Amazon Glacier, customers can store their data cost effectively
 * for months, years, or decades. Amazon Glacier also enables customers
 * to offload the administrative burdens of operating and scaling storage
 * to AWS, so they don't have to worry about capacity planning, hardware
 * provisioning, data replication, hardware failure and recovery, or
 * time-consuming hardware migrations.
 * </p>
 * <p>
 * Amazon Glacier is a great storage choice when low storage cost is
 * paramount, your data is rarely retrieved, and retrieval latency of
 * several hours is acceptable. If your application requires fast or
 * frequent access to your data, consider using Amazon S3. For more
 * information, go to
 * <a href="http://aws.amazon.com/s3/"> Amazon Simple Storage Service (Amazon S3) </a>
 * .
 * </p>
 * <p>
 * You can store any kind of data in any format. There is no maximum
 * limit on the total amount of data you can store in Amazon Glacier.
 * </p>
 * <p>
 * If you are a first-time user of Amazon Glacier, we recommend that you
 * begin by reading the following sections in the <i>Amazon Glacier
 * Developer Guide</i> :
 * </p>
 * 
 * <ul>
 * <li> <p>
 * 
 * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/introduction.html"> What is Amazon Glacier </a>
 * - This section of the Developer Guide describes the underlying data
 * model, the operations it supports, and the AWS SDKs that you can use
 * to interact with the service.
 * </p>
 * </li>
 * <li> <p>
 * 
 * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/amazon-glacier-getting-started.html"> Getting Started with Amazon Glacier </a>
 * - The Getting Started section walks you through the process of
 * creating a vault, uploading archives, creating jobs to download
 * archives, retrieving the job output, and deleting archives.
 * </p>
 * </li>
 * 
 * </ul>
 */
public interface AmazonGlacierAsync extends AmazonGlacier {
    /**
     * <p>
     * This operation lists all vaults owned by the calling user's account.
     * The list returned in the response is ASCII-sorted by vault name.
     * </p>
     * <p>
     * By default, this operation returns up to 1,000 items. If there are
     * more vaults to list, the response <code>marker</code> field contains
     * the vault Amazon Resource Name (ARN) at which to continue the list
     * with a new List Vaults request; otherwise, the <code>marker</code>
     * field is <code>null</code> . To return a list of vaults that begins at
     * a specific vault, set the <code>marker</code> request parameter to the
     * vault ARN you obtained from a previous List Vaults request. You can
     * also limit the number of vaults returned in the response by specifying
     * the <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-info.html"> Retrieving Vault Metadata in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vaults-get.html"> List Vaults </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listVaultsRequest Container for the necessary parameters to
     *           execute the ListVaults operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         ListVaults service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListVaultsResult> listVaultsAsync(ListVaultsRequest listVaultsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists all vaults owned by the calling user's account.
     * The list returned in the response is ASCII-sorted by vault name.
     * </p>
     * <p>
     * By default, this operation returns up to 1,000 items. If there are
     * more vaults to list, the response <code>marker</code> field contains
     * the vault Amazon Resource Name (ARN) at which to continue the list
     * with a new List Vaults request; otherwise, the <code>marker</code>
     * field is <code>null</code> . To return a list of vaults that begins at
     * a specific vault, set the <code>marker</code> request parameter to the
     * vault ARN you obtained from a previous List Vaults request. You can
     * also limit the number of vaults returned in the response by specifying
     * the <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-info.html"> Retrieving Vault Metadata in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vaults-get.html"> List Vaults </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listVaultsRequest Container for the necessary parameters to
     *           execute the ListVaults operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         ListVaults service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListVaultsResult> listVaultsAsync(ListVaultsRequest listVaultsRequest,
            AsyncHandler<ListVaultsRequest, ListVaultsResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns information about a job you previously
     * initiated, including the job initiation date, the user who initiated
     * the job, the job status code/message and the Amazon SNS topic to
     * notify after Amazon Glacier completes the job. For more information
     * about initiating a job, see InitiateJob.
     * </p>
     * <p>
     * <b>NOTE:</b> This operation enables you to check the status of your
     * job. However, it is strongly recommended that you set up an Amazon SNS
     * topic and specify it in your initiate job request so that Amazon
     * Glacier can notify the topic after it completes the job.
     * </p>
     * <p>
     * A job ID will not expire for at least 24 hours after Amazon Glacier
     * completes the job.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For information about the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-describe-job-get.html"> Working with Archives in Amazon Glacier </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param describeJobRequest Container for the necessary parameters to
     *           execute the DescribeJob operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DescribeJob service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<DescribeJobResult> describeJobAsync(DescribeJobRequest describeJobRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns information about a job you previously
     * initiated, including the job initiation date, the user who initiated
     * the job, the job status code/message and the Amazon SNS topic to
     * notify after Amazon Glacier completes the job. For more information
     * about initiating a job, see InitiateJob.
     * </p>
     * <p>
     * <b>NOTE:</b> This operation enables you to check the status of your
     * job. However, it is strongly recommended that you set up an Amazon SNS
     * topic and specify it in your initiate job request so that Amazon
     * Glacier can notify the topic after it completes the job.
     * </p>
     * <p>
     * A job ID will not expire for at least 24 hours after Amazon Glacier
     * completes the job.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For information about the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-describe-job-get.html"> Working with Archives in Amazon Glacier </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param describeJobRequest Container for the necessary parameters to
     *           execute the DescribeJob operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DescribeJob service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<DescribeJobResult> describeJobAsync(DescribeJobRequest describeJobRequest,
            AsyncHandler<DescribeJobRequest, DescribeJobResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation retrieves the <code>access-policy</code> subresource
     * set on the vault; for more information on setting this subresource,
     * see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-SetVaultAccessPolicy.html"> Set Vault Access Policy (PUT access-policy) </a> . If there is no access policy set on the vault, the operation returns a <code>404 Not found</code> error. For more information about vault access policies, see <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param getVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the GetVaultAccessPolicy operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         GetVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetVaultAccessPolicyResult> getVaultAccessPolicyAsync(GetVaultAccessPolicyRequest getVaultAccessPolicyRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation retrieves the <code>access-policy</code> subresource
     * set on the vault; for more information on setting this subresource,
     * see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-SetVaultAccessPolicy.html"> Set Vault Access Policy (PUT access-policy) </a> . If there is no access policy set on the vault, the operation returns a <code>404 Not found</code> error. For more information about vault access policies, see <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param getVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the GetVaultAccessPolicy operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         GetVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetVaultAccessPolicyResult> getVaultAccessPolicyAsync(GetVaultAccessPolicyRequest getVaultAccessPolicyRequest,
            AsyncHandler<GetVaultAccessPolicyRequest, GetVaultAccessPolicyResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation creates a new vault with the specified name. The name
     * of the vault must be unique within a region for an AWS account. You
     * can create up to 1,000 vaults per account. If you need to create more
     * vaults, contact Amazon Glacier.
     * </p>
     * <p>
     * You must use the following guidelines when naming a vault.
     * </p>
     * <p>
     * 
     * <ul>
     * <li> <p>
     * Names can be between 1 and 255 characters long.
     * </p>
     * </li>
     * <li> <p>
     * Allowed characters are a-z, A-Z, 0-9, '_' (underscore), '-' (hyphen),
     * and '.' (period).
     * </p>
     * </li>
     * 
     * </ul>
     * 
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/creating-vaults.html"> Creating a Vault in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-put.html"> Create Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param createVaultRequest Container for the necessary parameters to
     *           execute the CreateVault operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         CreateVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<CreateVaultResult> createVaultAsync(CreateVaultRequest createVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation creates a new vault with the specified name. The name
     * of the vault must be unique within a region for an AWS account. You
     * can create up to 1,000 vaults per account. If you need to create more
     * vaults, contact Amazon Glacier.
     * </p>
     * <p>
     * You must use the following guidelines when naming a vault.
     * </p>
     * <p>
     * 
     * <ul>
     * <li> <p>
     * Names can be between 1 and 255 characters long.
     * </p>
     * </li>
     * <li> <p>
     * Allowed characters are a-z, A-Z, 0-9, '_' (underscore), '-' (hyphen),
     * and '.' (period).
     * </p>
     * </li>
     * 
     * </ul>
     * 
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/creating-vaults.html"> Creating a Vault in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-put.html"> Create Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param createVaultRequest Container for the necessary parameters to
     *           execute the CreateVault operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         CreateVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<CreateVaultResult> createVaultAsync(CreateVaultRequest createVaultRequest,
            AsyncHandler<CreateVaultRequest, CreateVaultResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation initiates a multipart upload. Amazon Glacier creates a
     * multipart upload resource and returns its ID in the response. The
     * multipart upload ID is used in subsequent requests to upload parts of
     * an archive (see UploadMultipartPart).
     * </p>
     * <p>
     * When you initiate a multipart upload, you specify the part size in
     * number of bytes. The part size must be a megabyte (1024 KB) multiplied
     * by a power of 2-for example, 1048576 (1 MB), 2097152 (2 MB), 4194304
     * (4 MB), 8388608 (8 MB), and so on. The minimum allowable part size is
     * 1 MB, and the maximum is 4 GB.
     * </p>
     * <p>
     * Every part you upload to this resource (see UploadMultipartPart),
     * except the last one, must have the same size. The last one can be the
     * same size or smaller. For example, suppose you want to upload a 16.2
     * MB file. If you initiate the multipart upload with a part size of 4
     * MB, you will upload four parts of 4 MB each and one part of 0.2 MB.
     * </p>
     * <p>
     * <b>NOTE:</b> You don't need to know the size of the archive when you
     * start a multipart upload because Amazon Glacier does not require you
     * to specify the overall archive size.
     * </p>
     * <p>
     * After you complete the multipart upload, Amazon Glacier removes the
     * multipart upload resource referenced by the ID. Amazon Glacier also
     * removes the multipart upload resource if you cancel the multipart
     * upload or it may be removed if there is no activity for a period of 24
     * hours.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-initiate-upload.html"> Initiate Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param initiateMultipartUploadRequest Container for the necessary
     *           parameters to execute the InitiateMultipartUpload operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         InitiateMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<InitiateMultipartUploadResult> initiateMultipartUploadAsync(InitiateMultipartUploadRequest initiateMultipartUploadRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation initiates a multipart upload. Amazon Glacier creates a
     * multipart upload resource and returns its ID in the response. The
     * multipart upload ID is used in subsequent requests to upload parts of
     * an archive (see UploadMultipartPart).
     * </p>
     * <p>
     * When you initiate a multipart upload, you specify the part size in
     * number of bytes. The part size must be a megabyte (1024 KB) multiplied
     * by a power of 2-for example, 1048576 (1 MB), 2097152 (2 MB), 4194304
     * (4 MB), 8388608 (8 MB), and so on. The minimum allowable part size is
     * 1 MB, and the maximum is 4 GB.
     * </p>
     * <p>
     * Every part you upload to this resource (see UploadMultipartPart),
     * except the last one, must have the same size. The last one can be the
     * same size or smaller. For example, suppose you want to upload a 16.2
     * MB file. If you initiate the multipart upload with a part size of 4
     * MB, you will upload four parts of 4 MB each and one part of 0.2 MB.
     * </p>
     * <p>
     * <b>NOTE:</b> You don't need to know the size of the archive when you
     * start a multipart upload because Amazon Glacier does not require you
     * to specify the overall archive size.
     * </p>
     * <p>
     * After you complete the multipart upload, Amazon Glacier removes the
     * multipart upload resource referenced by the ID. Amazon Glacier also
     * removes the multipart upload resource if you cancel the multipart
     * upload or it may be removed if there is no activity for a period of 24
     * hours.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-initiate-upload.html"> Initiate Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param initiateMultipartUploadRequest Container for the necessary
     *           parameters to execute the InitiateMultipartUpload operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         InitiateMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<InitiateMultipartUploadResult> initiateMultipartUploadAsync(InitiateMultipartUploadRequest initiateMultipartUploadRequest,
            AsyncHandler<InitiateMultipartUploadRequest, InitiateMultipartUploadResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation initiates a job of the specified type. In this
     * release, you can initiate a job to retrieve either an archive or a
     * vault inventory (a list of archives in a vault).
     * </p>
     * <p>
     * Retrieving data from Amazon Glacier is a two-step process:
     * </p>
     * <ol> <li> <p>
     * Initiate a retrieval job.
     * </p>
     * <p>
     * <b>NOTE:</b> A data retrieval policy can cause your initiate
     * retrieval job request to fail with a PolicyEnforcedException
     * exception. For more information about data retrieval policies, see
     * Amazon Glacier Data Retrieval Policies. For more information about the
     * PolicyEnforcedException exception, see Error Responses.
     * </p>
     * </li>
     * <li> <p>
     * After the job completes, download the bytes.
     * </p>
     * </li>
     * </ol> <p>
     * The retrieval request is executed asynchronously. When you initiate a
     * retrieval job, Amazon Glacier creates a job and returns a job ID in
     * the response. When Amazon Glacier completes the job, you can get the
     * job output (archive or inventory data). For information about getting
     * job output, see GetJobOutput operation.
     * </p>
     * <p>
     * The job must complete before you can get its output. To determine
     * when a job is complete, you have the following options:
     * </p>
     * 
     * <ul>
     * <li> <p>
     * <b>Use Amazon SNS Notification</b> You can specify an Amazon Simple
     * Notification Service (Amazon SNS) topic to which Amazon Glacier can
     * post a notification after the job is completed. You can specify an SNS
     * topic per job request. The notification is sent only after Amazon
     * Glacier completes the job. In addition to specifying an SNS topic per
     * job request, you can configure vault notifications for a vault so that
     * job notifications are always sent. For more information, see
     * SetVaultNotifications.
     * </p>
     * </li>
     * <li> <p>
     * <b>Get job details</b> You can make a DescribeJob request to obtain
     * job status information while a job is in progress. However, it is more
     * efficient to use an Amazon SNS notification to determine when a job is
     * complete.
     * </p>
     * </li>
     * 
     * </ul>
     * <p>
     * <b>NOTE:</b> The information you get via notification is same that
     * you get by calling DescribeJob.
     * </p>
     * <p>
     * If for a specific event, you add both the notification configuration
     * on the vault and also specify an SNS topic in your initiate job
     * request, Amazon Glacier sends both notifications. For more
     * information, see SetVaultNotifications.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * <b>About the Vault Inventory</b>
     * </p>
     * <p>
     * Amazon Glacier prepares an inventory for each vault periodically,
     * every 24 hours. When you initiate a job for a vault inventory, Amazon
     * Glacier returns the last inventory for the vault. The inventory data
     * you get might be up to a day or two days old. Also, the initiate
     * inventory job might take some time to complete before you can download
     * the vault inventory. So you do not want to retrieve a vault inventory
     * for each vault operation. However, in some scenarios, you might find
     * the vault inventory useful. For example, when you upload an archive,
     * you can provide an archive description but not an archive name. Amazon
     * Glacier provides you a unique archive ID, an opaque string of
     * characters. So, you might maintain your own database that maps archive
     * names to their corresponding Amazon Glacier assigned archive IDs. You
     * might find the vault inventory useful in the event you need to
     * reconcile information in your database with the actual vault
     * inventory.
     * </p>
     * <p>
     * <b>Range Inventory Retrieval</b>
     * </p>
     * <p>
     * You can limit the number of inventory items retrieved by filtering on
     * the archive creation date or by setting a limit.
     * </p>
     * <p>
     * <i>Filtering by Archive Creation Date</i>
     * </p>
     * <p>
     * You can retrieve inventory items for archives created between
     * <code>StartDate</code> and <code>EndDate</code> by specifying values
     * for these parameters in the <b>InitiateJob</b> request. Archives
     * created on or after the <code>StartDate</code> and before the
     * <code>EndDate</code> will be returned. If you only provide the
     * <code>StartDate</code> without the <code>EndDate</code> , you will
     * retrieve the inventory for all archives created on or after the
     * <code>StartDate</code> . If you only provide the <code>EndDate</code>
     * without the <code>StartDate</code> , you will get back the inventory
     * for all archives created before the <code>EndDate</code> .
     * </p>
     * <p>
     * <i>Limiting Inventory Items per Retrieval</i>
     * </p>
     * <p>
     * You can limit the number of inventory items returned by setting the
     * <code>Limit</code> parameter in the <b>InitiateJob</b> request. The
     * inventory job output will contain inventory items up to the specified
     * <code>Limit</code> . If there are more inventory items available, the
     * result is paginated. After a job is complete you can use the
     * DescribeJob operation to get a marker that you use in a subsequent
     * <b>InitiateJob</b> request. The marker will indicate the starting
     * point to retrieve the next set of inventory items. You can page
     * through your entire inventory by repeatedly making <b>InitiateJob</b>
     * requests with the marker from the previous <b>DescribeJob</b> output,
     * until you get a marker from <b>DescribeJob</b> that returns null,
     * indicating that there are no more inventory items available.
     * </p>
     * <p>
     * You can use the <code>Limit</code> parameter together with the date
     * range parameters.
     * </p>
     * <p>
     * <b>About Ranged Archive Retrieval</b>
     * </p>
     * <p>
     * You can initiate an archive retrieval for the whole archive or a
     * range of the archive. In the case of ranged archive retrieval, you
     * specify a byte range to return or the whole archive. The range
     * specified must be megabyte (MB) aligned, that is the range start value
     * must be divisible by 1 MB and range end value plus 1 must be divisible
     * by 1 MB or equal the end of the archive. If the ranged archive
     * retrieval is not megabyte aligned, this operation returns a 400
     * response. Furthermore, to ensure you get checksum values for data you
     * download using Get Job Output API, the range must be tree hash
     * aligned.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-initiate-job-post.html"> Initiate a Job </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory </a>
     * 
     * </p>
     *
     * @param initiateJobRequest Container for the necessary parameters to
     *           execute the InitiateJob operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         InitiateJob service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<InitiateJobResult> initiateJobAsync(InitiateJobRequest initiateJobRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation initiates a job of the specified type. In this
     * release, you can initiate a job to retrieve either an archive or a
     * vault inventory (a list of archives in a vault).
     * </p>
     * <p>
     * Retrieving data from Amazon Glacier is a two-step process:
     * </p>
     * <ol> <li> <p>
     * Initiate a retrieval job.
     * </p>
     * <p>
     * <b>NOTE:</b> A data retrieval policy can cause your initiate
     * retrieval job request to fail with a PolicyEnforcedException
     * exception. For more information about data retrieval policies, see
     * Amazon Glacier Data Retrieval Policies. For more information about the
     * PolicyEnforcedException exception, see Error Responses.
     * </p>
     * </li>
     * <li> <p>
     * After the job completes, download the bytes.
     * </p>
     * </li>
     * </ol> <p>
     * The retrieval request is executed asynchronously. When you initiate a
     * retrieval job, Amazon Glacier creates a job and returns a job ID in
     * the response. When Amazon Glacier completes the job, you can get the
     * job output (archive or inventory data). For information about getting
     * job output, see GetJobOutput operation.
     * </p>
     * <p>
     * The job must complete before you can get its output. To determine
     * when a job is complete, you have the following options:
     * </p>
     * 
     * <ul>
     * <li> <p>
     * <b>Use Amazon SNS Notification</b> You can specify an Amazon Simple
     * Notification Service (Amazon SNS) topic to which Amazon Glacier can
     * post a notification after the job is completed. You can specify an SNS
     * topic per job request. The notification is sent only after Amazon
     * Glacier completes the job. In addition to specifying an SNS topic per
     * job request, you can configure vault notifications for a vault so that
     * job notifications are always sent. For more information, see
     * SetVaultNotifications.
     * </p>
     * </li>
     * <li> <p>
     * <b>Get job details</b> You can make a DescribeJob request to obtain
     * job status information while a job is in progress. However, it is more
     * efficient to use an Amazon SNS notification to determine when a job is
     * complete.
     * </p>
     * </li>
     * 
     * </ul>
     * <p>
     * <b>NOTE:</b> The information you get via notification is same that
     * you get by calling DescribeJob.
     * </p>
     * <p>
     * If for a specific event, you add both the notification configuration
     * on the vault and also specify an SNS topic in your initiate job
     * request, Amazon Glacier sends both notifications. For more
     * information, see SetVaultNotifications.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * <b>About the Vault Inventory</b>
     * </p>
     * <p>
     * Amazon Glacier prepares an inventory for each vault periodically,
     * every 24 hours. When you initiate a job for a vault inventory, Amazon
     * Glacier returns the last inventory for the vault. The inventory data
     * you get might be up to a day or two days old. Also, the initiate
     * inventory job might take some time to complete before you can download
     * the vault inventory. So you do not want to retrieve a vault inventory
     * for each vault operation. However, in some scenarios, you might find
     * the vault inventory useful. For example, when you upload an archive,
     * you can provide an archive description but not an archive name. Amazon
     * Glacier provides you a unique archive ID, an opaque string of
     * characters. So, you might maintain your own database that maps archive
     * names to their corresponding Amazon Glacier assigned archive IDs. You
     * might find the vault inventory useful in the event you need to
     * reconcile information in your database with the actual vault
     * inventory.
     * </p>
     * <p>
     * <b>Range Inventory Retrieval</b>
     * </p>
     * <p>
     * You can limit the number of inventory items retrieved by filtering on
     * the archive creation date or by setting a limit.
     * </p>
     * <p>
     * <i>Filtering by Archive Creation Date</i>
     * </p>
     * <p>
     * You can retrieve inventory items for archives created between
     * <code>StartDate</code> and <code>EndDate</code> by specifying values
     * for these parameters in the <b>InitiateJob</b> request. Archives
     * created on or after the <code>StartDate</code> and before the
     * <code>EndDate</code> will be returned. If you only provide the
     * <code>StartDate</code> without the <code>EndDate</code> , you will
     * retrieve the inventory for all archives created on or after the
     * <code>StartDate</code> . If you only provide the <code>EndDate</code>
     * without the <code>StartDate</code> , you will get back the inventory
     * for all archives created before the <code>EndDate</code> .
     * </p>
     * <p>
     * <i>Limiting Inventory Items per Retrieval</i>
     * </p>
     * <p>
     * You can limit the number of inventory items returned by setting the
     * <code>Limit</code> parameter in the <b>InitiateJob</b> request. The
     * inventory job output will contain inventory items up to the specified
     * <code>Limit</code> . If there are more inventory items available, the
     * result is paginated. After a job is complete you can use the
     * DescribeJob operation to get a marker that you use in a subsequent
     * <b>InitiateJob</b> request. The marker will indicate the starting
     * point to retrieve the next set of inventory items. You can page
     * through your entire inventory by repeatedly making <b>InitiateJob</b>
     * requests with the marker from the previous <b>DescribeJob</b> output,
     * until you get a marker from <b>DescribeJob</b> that returns null,
     * indicating that there are no more inventory items available.
     * </p>
     * <p>
     * You can use the <code>Limit</code> parameter together with the date
     * range parameters.
     * </p>
     * <p>
     * <b>About Ranged Archive Retrieval</b>
     * </p>
     * <p>
     * You can initiate an archive retrieval for the whole archive or a
     * range of the archive. In the case of ranged archive retrieval, you
     * specify a byte range to return or the whole archive. The range
     * specified must be megabyte (MB) aligned, that is the range start value
     * must be divisible by 1 MB and range end value plus 1 must be divisible
     * by 1 MB or equal the end of the archive. If the ranged archive
     * retrieval is not megabyte aligned, this operation returns a 400
     * response. Furthermore, to ensure you get checksum values for data you
     * download using Get Job Output API, the range must be tree hash
     * aligned.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-initiate-job-post.html"> Initiate a Job </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory </a>
     * 
     * </p>
     *
     * @param initiateJobRequest Container for the necessary parameters to
     *           execute the InitiateJob operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         InitiateJob service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<InitiateJobResult> initiateJobAsync(InitiateJobRequest initiateJobRequest,
            AsyncHandler<InitiateJobRequest, InitiateJobResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation adds an archive to a vault. This is a synchronous
     * operation, and for a successful upload, your data is durably
     * persisted. Amazon Glacier returns the archive ID in the
     * <code>x-amz-archive-id</code> header of the response.
     * </p>
     * <p>
     * You must use the archive ID to access your data in Amazon Glacier.
     * After you upload an archive, you should save the archive ID returned
     * so that you can retrieve or delete the archive later. Besides saving
     * the archive ID, you can also index it and give it a friendly name to
     * allow for better searching. You can also use the optional archive
     * description field to specify how the archive is referred to in an
     * external index of archives, such as you might create in Amazon
     * DynamoDB. You can also get the vault inventory to obtain a list of
     * archive IDs in a vault. For more information, see InitiateJob.
     * </p>
     * <p>
     * You must provide a SHA256 tree hash of the data you are uploading.
     * For information about computing a SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * .
     * </p>
     * <p>
     * You can optionally specify an archive description of up to 1,024
     * printable ASCII characters. You can get the archive description when
     * you either retrieve the archive or get the vault inventory. For more
     * information, see InitiateJob. Amazon Glacier does not interpret the
     * description in any way. An archive description does not need to be
     * unique. You cannot use the description to retrieve or sort the archive
     * list.
     * </p>
     * <p>
     * Archives are immutable. After you upload an archive, you cannot edit
     * the archive or its description.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-an-archive.html"> Uploading an Archive in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-post.html"> Upload Archive </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param uploadArchiveRequest Container for the necessary parameters to
     *           execute the UploadArchive operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         UploadArchive service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<UploadArchiveResult> uploadArchiveAsync(UploadArchiveRequest uploadArchiveRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation adds an archive to a vault. This is a synchronous
     * operation, and for a successful upload, your data is durably
     * persisted. Amazon Glacier returns the archive ID in the
     * <code>x-amz-archive-id</code> header of the response.
     * </p>
     * <p>
     * You must use the archive ID to access your data in Amazon Glacier.
     * After you upload an archive, you should save the archive ID returned
     * so that you can retrieve or delete the archive later. Besides saving
     * the archive ID, you can also index it and give it a friendly name to
     * allow for better searching. You can also use the optional archive
     * description field to specify how the archive is referred to in an
     * external index of archives, such as you might create in Amazon
     * DynamoDB. You can also get the vault inventory to obtain a list of
     * archive IDs in a vault. For more information, see InitiateJob.
     * </p>
     * <p>
     * You must provide a SHA256 tree hash of the data you are uploading.
     * For information about computing a SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * .
     * </p>
     * <p>
     * You can optionally specify an archive description of up to 1,024
     * printable ASCII characters. You can get the archive description when
     * you either retrieve the archive or get the vault inventory. For more
     * information, see InitiateJob. Amazon Glacier does not interpret the
     * description in any way. An archive description does not need to be
     * unique. You cannot use the description to retrieve or sort the archive
     * list.
     * </p>
     * <p>
     * Archives are immutable. After you upload an archive, you cannot edit
     * the archive or its description.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-an-archive.html"> Uploading an Archive in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-post.html"> Upload Archive </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param uploadArchiveRequest Container for the necessary parameters to
     *           execute the UploadArchive operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         UploadArchive service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<UploadArchiveResult> uploadArchiveAsync(UploadArchiveRequest uploadArchiveRequest,
            AsyncHandler<UploadArchiveRequest, UploadArchiveResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation configures notifications that will be sent when
     * specific events happen to a vault. By default, you don't get any
     * notifications.
     * </p>
     * <p>
     * To configure vault notifications, send a PUT request to the
     * <code>notification-configuration</code> subresource of the vault. The
     * request should include a JSON document that provides an Amazon SNS
     * topic and specific events for which you want Amazon Glacier to send
     * notifications to the topic.
     * </p>
     * <p>
     * Amazon SNS topics must grant permission to the vault to be allowed to
     * publish notifications to the topic. You can configure a vault to
     * publish a notification for the following vault events:
     * </p>
     * 
     * <ul>
     * <li> <b>ArchiveRetrievalCompleted</b> This event occurs when a job
     * that was initiated for an archive retrieval is completed
     * (InitiateJob). The status of the completed job can be "Succeeded" or
     * "Failed". The notification sent to the SNS topic is the same output as
     * returned from DescribeJob. </li>
     * <li> <b>InventoryRetrievalCompleted</b> This event occurs when a job
     * that was initiated for an inventory retrieval is completed
     * (InitiateJob). The status of the completed job can be "Succeeded" or
     * "Failed". The notification sent to the SNS topic is the same output as
     * returned from DescribeJob. </li>
     * 
     * </ul>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-put.html"> Set Vault Notification Configuration </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param setVaultNotificationsRequest Container for the necessary
     *           parameters to execute the SetVaultNotifications operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         SetVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setVaultNotificationsAsync(SetVaultNotificationsRequest setVaultNotificationsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation configures notifications that will be sent when
     * specific events happen to a vault. By default, you don't get any
     * notifications.
     * </p>
     * <p>
     * To configure vault notifications, send a PUT request to the
     * <code>notification-configuration</code> subresource of the vault. The
     * request should include a JSON document that provides an Amazon SNS
     * topic and specific events for which you want Amazon Glacier to send
     * notifications to the topic.
     * </p>
     * <p>
     * Amazon SNS topics must grant permission to the vault to be allowed to
     * publish notifications to the topic. You can configure a vault to
     * publish a notification for the following vault events:
     * </p>
     * 
     * <ul>
     * <li> <b>ArchiveRetrievalCompleted</b> This event occurs when a job
     * that was initiated for an archive retrieval is completed
     * (InitiateJob). The status of the completed job can be "Succeeded" or
     * "Failed". The notification sent to the SNS topic is the same output as
     * returned from DescribeJob. </li>
     * <li> <b>InventoryRetrievalCompleted</b> This event occurs when a job
     * that was initiated for an inventory retrieval is completed
     * (InitiateJob). The status of the completed job can be "Succeeded" or
     * "Failed". The notification sent to the SNS topic is the same output as
     * returned from DescribeJob. </li>
     * 
     * </ul>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-put.html"> Set Vault Notification Configuration </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param setVaultNotificationsRequest Container for the necessary
     *           parameters to execute the SetVaultNotifications operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         SetVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setVaultNotificationsAsync(SetVaultNotificationsRequest setVaultNotificationsRequest,
            AsyncHandler<SetVaultNotificationsRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns information about a vault, including the
     * vault's Amazon Resource Name (ARN), the date the vault was created,
     * the number of archives it contains, and the total size of all the
     * archives in the vault. The number of archives and their total size are
     * as of the last inventory generation. This means that if you add or
     * remove an archive from a vault, and then immediately use Describe
     * Vault, the change in contents will not be immediately reflected. If
     * you want to retrieve the latest inventory of the vault, use
     * InitiateJob. Amazon Glacier generates vault inventories approximately
     * daily. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory in Amazon Glacier </a>
     * .
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-info.html"> Retrieving Vault Metadata in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-get.html"> Describe Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param describeVaultRequest Container for the necessary parameters to
     *           execute the DescribeVault operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DescribeVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<DescribeVaultResult> describeVaultAsync(DescribeVaultRequest describeVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns information about a vault, including the
     * vault's Amazon Resource Name (ARN), the date the vault was created,
     * the number of archives it contains, and the total size of all the
     * archives in the vault. The number of archives and their total size are
     * as of the last inventory generation. This means that if you add or
     * remove an archive from a vault, and then immediately use Describe
     * Vault, the change in contents will not be immediately reflected. If
     * you want to retrieve the latest inventory of the vault, use
     * InitiateJob. Amazon Glacier generates vault inventories approximately
     * daily. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory in Amazon Glacier </a>
     * .
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/retrieving-vault-info.html"> Retrieving Vault Metadata in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-get.html"> Describe Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param describeVaultRequest Container for the necessary parameters to
     *           execute the DescribeVault operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DescribeVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<DescribeVaultResult> describeVaultAsync(DescribeVaultRequest describeVaultRequest,
            AsyncHandler<DescribeVaultRequest, DescribeVaultResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes the notification configuration set for a
     * vault. The operation is eventually consistent; that is, it might take
     * some time for Amazon Glacier to completely disable the notifications
     * and you might still receive some notifications for a short time after
     * you send the delete request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-delete.html"> Delete Vault Notification Configuration </a>
     * in the Amazon Glacier Developer Guide.
     * </p>
     *
     * @param deleteVaultNotificationsRequest Container for the necessary
     *           parameters to execute the DeleteVaultNotifications operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultNotificationsAsync(DeleteVaultNotificationsRequest deleteVaultNotificationsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes the notification configuration set for a
     * vault. The operation is eventually consistent; that is, it might take
     * some time for Amazon Glacier to completely disable the notifications
     * and you might still receive some notifications for a short time after
     * you send the delete request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-delete.html"> Delete Vault Notification Configuration </a>
     * in the Amazon Glacier Developer Guide.
     * </p>
     *
     * @param deleteVaultNotificationsRequest Container for the necessary
     *           parameters to execute the DeleteVaultNotifications operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultNotificationsAsync(DeleteVaultNotificationsRequest deleteVaultNotificationsRequest,
            AsyncHandler<DeleteVaultNotificationsRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation adds the specified tags to a vault. Each tag is
     * composed of a key and a value. Each vault can have up to 10 tags. If
     * your request would cause the tag limit for the vault to be exceeded,
     * the operation throws the <code>LimitExceededException</code> error. If
     * a tag already exists on the vault under a specified key, the existing
     * key value will be overwritten. For more information about tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * .
     * </p>
     *
     * @param addTagsToVaultRequest Container for the necessary parameters to
     *           execute the AddTagsToVault operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         AddTagsToVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> addTagsToVaultAsync(AddTagsToVaultRequest addTagsToVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation adds the specified tags to a vault. Each tag is
     * composed of a key and a value. Each vault can have up to 10 tags. If
     * your request would cause the tag limit for the vault to be exceeded,
     * the operation throws the <code>LimitExceededException</code> error. If
     * a tag already exists on the vault under a specified key, the existing
     * key value will be overwritten. For more information about tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * .
     * </p>
     *
     * @param addTagsToVaultRequest Container for the necessary parameters to
     *           execute the AddTagsToVault operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         AddTagsToVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> addTagsToVaultAsync(AddTagsToVaultRequest addTagsToVaultRequest,
            AsyncHandler<AddTagsToVaultRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns the current data retrieval policy for the
     * account and region specified in the GET request. For more information
     * about data retrieval policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/data-retrieval-policy.html"> Amazon Glacier Data Retrieval Policies </a>
     * .
     * </p>
     *
     * @param getDataRetrievalPolicyRequest Container for the necessary
     *           parameters to execute the GetDataRetrievalPolicy operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         GetDataRetrievalPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetDataRetrievalPolicyResult> getDataRetrievalPolicyAsync(GetDataRetrievalPolicyRequest getDataRetrievalPolicyRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation returns the current data retrieval policy for the
     * account and region specified in the GET request. For more information
     * about data retrieval policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/data-retrieval-policy.html"> Amazon Glacier Data Retrieval Policies </a>
     * .
     * </p>
     *
     * @param getDataRetrievalPolicyRequest Container for the necessary
     *           parameters to execute the GetDataRetrievalPolicy operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         GetDataRetrievalPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetDataRetrievalPolicyResult> getDataRetrievalPolicyAsync(GetDataRetrievalPolicyRequest getDataRetrievalPolicyRequest,
            AsyncHandler<GetDataRetrievalPolicyRequest, GetDataRetrievalPolicyResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists all the tags attached to a vault. The operation
     * returns an empty map if there are no tags. For more information about
     * tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * .
     * </p>
     *
     * @param listTagsForVaultRequest Container for the necessary parameters
     *           to execute the ListTagsForVault operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         ListTagsForVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListTagsForVaultResult> listTagsForVaultAsync(ListTagsForVaultRequest listTagsForVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists all the tags attached to a vault. The operation
     * returns an empty map if there are no tags. For more information about
     * tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * .
     * </p>
     *
     * @param listTagsForVaultRequest Container for the necessary parameters
     *           to execute the ListTagsForVault operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         ListTagsForVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListTagsForVaultResult> listTagsForVaultAsync(ListTagsForVaultRequest listTagsForVaultRequest,
            AsyncHandler<ListTagsForVaultRequest, ListTagsForVaultResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation retrieves the <code>notification-configuration</code>
     * subresource of the specified vault.
     * </p>
     * <p>
     * For information about setting a notification configuration on a
     * vault, see SetVaultNotifications. If a notification configuration for
     * a vault is not set, the operation returns a <code>404 Not Found</code>
     * error. For more information about vault notifications, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a>
     * .
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-get.html"> Get Vault Notification Configuration </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param getVaultNotificationsRequest Container for the necessary
     *           parameters to execute the GetVaultNotifications operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         GetVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetVaultNotificationsResult> getVaultNotificationsAsync(GetVaultNotificationsRequest getVaultNotificationsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation retrieves the <code>notification-configuration</code>
     * subresource of the specified vault.
     * </p>
     * <p>
     * For information about setting a notification configuration on a
     * vault, see SetVaultNotifications. If a notification configuration for
     * a vault is not set, the operation returns a <code>404 Not Found</code>
     * error. For more information about vault notifications, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a>
     * .
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/configuring-notifications.html"> Configuring Vault Notifications in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-notifications-get.html"> Get Vault Notification Configuration </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param getVaultNotificationsRequest Container for the necessary
     *           parameters to execute the GetVaultNotifications operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         GetVaultNotifications service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetVaultNotificationsResult> getVaultNotificationsAsync(GetVaultNotificationsRequest getVaultNotificationsRequest,
            AsyncHandler<GetVaultNotificationsRequest, GetVaultNotificationsResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists the parts of an archive that have been uploaded
     * in a specific multipart upload. You can make this request at any time
     * during an in-progress multipart upload before you complete the upload
     * (see CompleteMultipartUpload. List Parts returns an error for
     * completed uploads. The list returned in the List Parts response is
     * sorted by part range.
     * </p>
     * <p>
     * The List Parts operation supports pagination. By default, this
     * operation returns up to 1,000 uploaded parts in the response. You
     * should always check the response for a <code>marker</code> at which to
     * continue the list; if there are no more items the <code>marker</code>
     * is <code>null</code> . To return a list of parts that begins at a
     * specific part, set the <code>marker</code> request parameter to the
     * value you obtained from a previous List Parts request. You can also
     * limit the number of parts returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-list-parts.html"> List Parts </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listPartsRequest Container for the necessary parameters to
     *           execute the ListParts operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         ListParts service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListPartsResult> listPartsAsync(ListPartsRequest listPartsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists the parts of an archive that have been uploaded
     * in a specific multipart upload. You can make this request at any time
     * during an in-progress multipart upload before you complete the upload
     * (see CompleteMultipartUpload. List Parts returns an error for
     * completed uploads. The list returned in the List Parts response is
     * sorted by part range.
     * </p>
     * <p>
     * The List Parts operation supports pagination. By default, this
     * operation returns up to 1,000 uploaded parts in the response. You
     * should always check the response for a <code>marker</code> at which to
     * continue the list; if there are no more items the <code>marker</code>
     * is <code>null</code> . To return a list of parts that begins at a
     * specific part, set the <code>marker</code> request parameter to the
     * value you obtained from a previous List Parts request. You can also
     * limit the number of parts returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-list-parts.html"> List Parts </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listPartsRequest Container for the necessary parameters to
     *           execute the ListParts operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         ListParts service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListPartsResult> listPartsAsync(ListPartsRequest listPartsRequest,
            AsyncHandler<ListPartsRequest, ListPartsResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists jobs for a vault, including jobs that are
     * in-progress and jobs that have recently finished.
     * </p>
     * <p>
     * <b>NOTE:</b> Amazon Glacier retains recently completed jobs for a
     * period before deleting them; however, it eventually removes completed
     * jobs. The output of completed jobs can be retrieved. Retaining
     * completed jobs for a period of time after they have completed enables
     * you to get a job output in the event you miss the job completion
     * notification or your first attempt to download it fails. For example,
     * suppose you start an archive retrieval job to download an archive.
     * After the job completes, you start to download the archive but
     * encounter a network error. In this scenario, you can retry and
     * download the archive while the job exists.
     * </p>
     * <p>
     * To retrieve an archive or retrieve a vault inventory from Amazon
     * Glacier, you first initiate a job, and after the job completes, you
     * download the data. For an archive retrieval, the output is the archive
     * data, and for an inventory retrieval, it is the inventory list. The
     * List Job operation returns a list of these jobs sorted by job
     * initiation time.
     * </p>
     * <p>
     * This List Jobs operation supports pagination. By default, this
     * operation returns up to 1,000 jobs in the response. You should always
     * check the response for a <code>marker</code> at which to continue the
     * list; if there are no more items the <code>marker</code> is
     * <code>null</code> . To return a list of jobs that begins at a specific
     * job, set the <code>marker</code> request parameter to the value you
     * obtained from a previous List Jobs request. You can also limit the
     * number of jobs returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * Additionally, you can filter the jobs list returned by specifying an
     * optional <code>statuscode</code> (InProgress, Succeeded, or Failed)
     * and <code>completed</code> (true, false) parameter. The
     * <code>statuscode</code> allows you to specify that only jobs that
     * match a specified status are returned. The <code>completed</code>
     * parameter allows you to specify that only jobs in a specific
     * completion state are returned.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-jobs-get.html"> List Jobs </a>
     * 
     * </p>
     *
     * @param listJobsRequest Container for the necessary parameters to
     *           execute the ListJobs operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the ListJobs
     *         service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListJobsResult> listJobsAsync(ListJobsRequest listJobsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists jobs for a vault, including jobs that are
     * in-progress and jobs that have recently finished.
     * </p>
     * <p>
     * <b>NOTE:</b> Amazon Glacier retains recently completed jobs for a
     * period before deleting them; however, it eventually removes completed
     * jobs. The output of completed jobs can be retrieved. Retaining
     * completed jobs for a period of time after they have completed enables
     * you to get a job output in the event you miss the job completion
     * notification or your first attempt to download it fails. For example,
     * suppose you start an archive retrieval job to download an archive.
     * After the job completes, you start to download the archive but
     * encounter a network error. In this scenario, you can retry and
     * download the archive while the job exists.
     * </p>
     * <p>
     * To retrieve an archive or retrieve a vault inventory from Amazon
     * Glacier, you first initiate a job, and after the job completes, you
     * download the data. For an archive retrieval, the output is the archive
     * data, and for an inventory retrieval, it is the inventory list. The
     * List Job operation returns a list of these jobs sorted by job
     * initiation time.
     * </p>
     * <p>
     * This List Jobs operation supports pagination. By default, this
     * operation returns up to 1,000 jobs in the response. You should always
     * check the response for a <code>marker</code> at which to continue the
     * list; if there are no more items the <code>marker</code> is
     * <code>null</code> . To return a list of jobs that begins at a specific
     * job, set the <code>marker</code> request parameter to the value you
     * obtained from a previous List Jobs request. You can also limit the
     * number of jobs returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * Additionally, you can filter the jobs list returned by specifying an
     * optional <code>statuscode</code> (InProgress, Succeeded, or Failed)
     * and <code>completed</code> (true, false) parameter. The
     * <code>statuscode</code> allows you to specify that only jobs that
     * match a specified status are returned. The <code>completed</code>
     * parameter allows you to specify that only jobs in a specific
     * completion state are returned.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-jobs-get.html"> List Jobs </a>
     * 
     * </p>
     *
     * @param listJobsRequest Container for the necessary parameters to
     *           execute the ListJobs operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the ListJobs
     *         service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListJobsResult> listJobsAsync(ListJobsRequest listJobsRequest,
            AsyncHandler<ListJobsRequest, ListJobsResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation aborts a multipart upload identified by the upload ID.
     * </p>
     * <p>
     * After the Abort Multipart Upload request succeeds, you cannot upload
     * any more parts to the multipart upload or complete the multipart
     * upload. Aborting a completed upload fails. However, aborting an
     * already-aborted upload will succeed, for a short time. For more
     * information about uploading a part and completing a multipart upload,
     * see UploadMultipartPart and CompleteMultipartUpload.
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-abort-upload.html"> Abort Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param abortMultipartUploadRequest Container for the necessary
     *           parameters to execute the AbortMultipartUpload operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         AbortMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> abortMultipartUploadAsync(AbortMultipartUploadRequest abortMultipartUploadRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation aborts a multipart upload identified by the upload ID.
     * </p>
     * <p>
     * After the Abort Multipart Upload request succeeds, you cannot upload
     * any more parts to the multipart upload or complete the multipart
     * upload. Aborting a completed upload fails. However, aborting an
     * already-aborted upload will succeed, for a short time. For more
     * information about uploading a part and completing a multipart upload,
     * see UploadMultipartPart and CompleteMultipartUpload.
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-abort-upload.html"> Abort Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param abortMultipartUploadRequest Container for the necessary
     *           parameters to execute the AbortMultipartUpload operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         AbortMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> abortMultipartUploadAsync(AbortMultipartUploadRequest abortMultipartUploadRequest,
            AsyncHandler<AbortMultipartUploadRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes an archive from a vault. Subsequent requests
     * to initiate a retrieval of this archive will fail. Archive retrievals
     * that are in progress for this archive ID may or may not succeed
     * according to the following scenarios:
     * </p>
     * 
     * <ul>
     * <li>If the archive retrieval job is actively preparing the data for
     * download when Amazon Glacier receives the delete archive request, the
     * archival retrieval operation might fail. </li>
     * <li>If the archive retrieval job has successfully prepared the
     * archive for download when Amazon Glacier receives the delete archive
     * request, you will be able to download the output. </li>
     * 
     * </ul>
     * <p>
     * This operation is idempotent. Attempting to delete an already-deleted
     * archive does not result in an error.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-an-archive.html"> Deleting an Archive in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-delete.html"> Delete Archive </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param deleteArchiveRequest Container for the necessary parameters to
     *           execute the DeleteArchive operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteArchive service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteArchiveAsync(DeleteArchiveRequest deleteArchiveRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes an archive from a vault. Subsequent requests
     * to initiate a retrieval of this archive will fail. Archive retrievals
     * that are in progress for this archive ID may or may not succeed
     * according to the following scenarios:
     * </p>
     * 
     * <ul>
     * <li>If the archive retrieval job is actively preparing the data for
     * download when Amazon Glacier receives the delete archive request, the
     * archival retrieval operation might fail. </li>
     * <li>If the archive retrieval job has successfully prepared the
     * archive for download when Amazon Glacier receives the delete archive
     * request, you will be able to download the output. </li>
     * 
     * </ul>
     * <p>
     * This operation is idempotent. Attempting to delete an already-deleted
     * archive does not result in an error.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-an-archive.html"> Deleting an Archive in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-delete.html"> Delete Archive </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param deleteArchiveRequest Container for the necessary parameters to
     *           execute the DeleteArchive operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteArchive service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteArchiveAsync(DeleteArchiveRequest deleteArchiveRequest,
            AsyncHandler<DeleteArchiveRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation downloads the output of the job you initiated using
     * InitiateJob. Depending on the job type you specified when you
     * initiated the job, the output will be either the content of an archive
     * or a vault inventory.
     * </p>
     * <p>
     * A job ID will not expire for at least 24 hours after Amazon Glacier
     * completes the job. That is, you can download the job output within the
     * 24 hours period after Amazon Glacier completes the job.
     * </p>
     * <p>
     * If the job output is large, then you can use the <code>Range</code>
     * request header to retrieve a portion of the output. This allows you to
     * download the entire output in smaller chunks of bytes. For example,
     * suppose you have 1 GB of job output you want to download and you
     * decide to download 128 MB chunks of data at a time, which is a total
     * of eight Get Job Output requests. You use the following process to
     * download the job output:
     * </p>
     * <ol> <li> <p>
     * Download a 128 MB chunk of output by specifying the appropriate byte
     * range using the <code>Range</code> header.
     * </p>
     * </li>
     * <li> <p>
     * Along with the data, the response includes a SHA256 tree hash of the
     * payload. You compute the checksum of the payload on the client and
     * compare it with the checksum you received in the response to ensure
     * you received all the expected data.
     * </p>
     * </li>
     * <li> <p>
     * Repeat steps 1 and 2 for all the eight 128 MB chunks of output data,
     * each time specifying the appropriate byte range.
     * </p>
     * </li>
     * <li> <p>
     * After downloading all the parts of the job output, you have a list of
     * eight checksum values. Compute the tree hash of these values to find
     * the checksum of the entire output. Using the DescribeJob API, obtain
     * job information of the job that provided you the output. The response
     * includes the checksum of the entire archive stored in Amazon Glacier.
     * You compare this value with the checksum you computed to ensure you
     * have downloaded the entire archive content with no errors.
     * </p>
     * </li>
     * </ol> <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory </a> , <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/downloading-an-archive.html"> Downloading an Archive </a> , and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-job-output-get.html"> Get Job Output </a>
     * 
     * </p>
     *
     * @param getJobOutputRequest Container for the necessary parameters to
     *           execute the GetJobOutput operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         GetJobOutput service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetJobOutputResult> getJobOutputAsync(GetJobOutputRequest getJobOutputRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation downloads the output of the job you initiated using
     * InitiateJob. Depending on the job type you specified when you
     * initiated the job, the output will be either the content of an archive
     * or a vault inventory.
     * </p>
     * <p>
     * A job ID will not expire for at least 24 hours after Amazon Glacier
     * completes the job. That is, you can download the job output within the
     * 24 hours period after Amazon Glacier completes the job.
     * </p>
     * <p>
     * If the job output is large, then you can use the <code>Range</code>
     * request header to retrieve a portion of the output. This allows you to
     * download the entire output in smaller chunks of bytes. For example,
     * suppose you have 1 GB of job output you want to download and you
     * decide to download 128 MB chunks of data at a time, which is a total
     * of eight Get Job Output requests. You use the following process to
     * download the job output:
     * </p>
     * <ol> <li> <p>
     * Download a 128 MB chunk of output by specifying the appropriate byte
     * range using the <code>Range</code> header.
     * </p>
     * </li>
     * <li> <p>
     * Along with the data, the response includes a SHA256 tree hash of the
     * payload. You compute the checksum of the payload on the client and
     * compare it with the checksum you received in the response to ensure
     * you received all the expected data.
     * </p>
     * </li>
     * <li> <p>
     * Repeat steps 1 and 2 for all the eight 128 MB chunks of output data,
     * each time specifying the appropriate byte range.
     * </p>
     * </li>
     * <li> <p>
     * After downloading all the parts of the job output, you have a list of
     * eight checksum values. Compute the tree hash of these values to find
     * the checksum of the entire output. Using the DescribeJob API, obtain
     * job information of the job that provided you the output. The response
     * includes the checksum of the entire archive stored in Amazon Glacier.
     * You compare this value with the checksum you computed to ensure you
     * have downloaded the entire archive content with no errors.
     * </p>
     * </li>
     * </ol> <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-inventory.html"> Downloading a Vault Inventory </a> , <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/downloading-an-archive.html"> Downloading an Archive </a> , and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-job-output-get.html"> Get Job Output </a>
     * 
     * </p>
     *
     * @param getJobOutputRequest Container for the necessary parameters to
     *           execute the GetJobOutput operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         GetJobOutput service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<GetJobOutputResult> getJobOutputAsync(GetJobOutputRequest getJobOutputRequest,
            AsyncHandler<GetJobOutputRequest, GetJobOutputResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * You call this operation to inform Amazon Glacier that all the archive
     * parts have been uploaded and that Amazon Glacier can now assemble the
     * archive from the uploaded parts. After assembling and saving the
     * archive to the vault, Amazon Glacier returns the URI path of the newly
     * created archive resource. Using the URI path, you can then access the
     * archive. After you upload an archive, you should save the archive ID
     * returned to retrieve the archive at a later point. You can also get
     * the vault inventory to obtain a list of archive IDs in a vault. For
     * more information, see InitiateJob.
     * </p>
     * <p>
     * In the request, you must include the computed SHA256 tree hash of the
     * entire archive you have uploaded. For information about computing a
     * SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * . On the server side, Amazon Glacier also constructs the SHA256 tree
     * hash of the assembled archive. If the values match, Amazon Glacier
     * saves the archive to the vault; otherwise, it returns an error, and
     * the operation fails. The ListParts operation returns a list of parts
     * uploaded for a specific multipart upload. It includes checksum
     * information for each uploaded part that can be used to debug a bad
     * checksum issue.
     * </p>
     * <p>
     * Additionally, Amazon Glacier also checks for any missing content
     * ranges when assembling the archive, if missing content ranges are
     * found, Amazon Glacier returns an error and the operation fails.
     * </p>
     * <p>
     * Complete Multipart Upload is an idempotent operation. After your
     * first successful complete multipart upload, if you call the operation
     * again within a short period, the operation will succeed and return the
     * same archive ID. This is useful in the event you experience a network
     * issue that causes an aborted connection or receive a 500 server error,
     * in which case you can repeat your Complete Multipart Upload request
     * and get the same archive ID without creating duplicate archives. Note,
     * however, that after the multipart upload completes, you cannot call
     * the List Parts operation and the multipart upload will not appear in
     * List Multipart Uploads response, even if idempotent complete is
     * possible.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-complete-upload.html"> Complete Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param completeMultipartUploadRequest Container for the necessary
     *           parameters to execute the CompleteMultipartUpload operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         CompleteMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<CompleteMultipartUploadResult> completeMultipartUploadAsync(CompleteMultipartUploadRequest completeMultipartUploadRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * You call this operation to inform Amazon Glacier that all the archive
     * parts have been uploaded and that Amazon Glacier can now assemble the
     * archive from the uploaded parts. After assembling and saving the
     * archive to the vault, Amazon Glacier returns the URI path of the newly
     * created archive resource. Using the URI path, you can then access the
     * archive. After you upload an archive, you should save the archive ID
     * returned to retrieve the archive at a later point. You can also get
     * the vault inventory to obtain a list of archive IDs in a vault. For
     * more information, see InitiateJob.
     * </p>
     * <p>
     * In the request, you must include the computed SHA256 tree hash of the
     * entire archive you have uploaded. For information about computing a
     * SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * . On the server side, Amazon Glacier also constructs the SHA256 tree
     * hash of the assembled archive. If the values match, Amazon Glacier
     * saves the archive to the vault; otherwise, it returns an error, and
     * the operation fails. The ListParts operation returns a list of parts
     * uploaded for a specific multipart upload. It includes checksum
     * information for each uploaded part that can be used to debug a bad
     * checksum issue.
     * </p>
     * <p>
     * Additionally, Amazon Glacier also checks for any missing content
     * ranges when assembling the archive, if missing content ranges are
     * found, Amazon Glacier returns an error and the operation fails.
     * </p>
     * <p>
     * Complete Multipart Upload is an idempotent operation. After your
     * first successful complete multipart upload, if you call the operation
     * again within a short period, the operation will succeed and return the
     * same archive ID. This is useful in the event you experience a network
     * issue that causes an aborted connection or receive a 500 server error,
     * in which case you can repeat your Complete Multipart Upload request
     * and get the same archive ID without creating duplicate archives. Note,
     * however, that after the multipart upload completes, you cannot call
     * the List Parts operation and the multipart upload will not appear in
     * List Multipart Uploads response, even if idempotent complete is
     * possible.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-complete-upload.html"> Complete Multipart Upload </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param completeMultipartUploadRequest Container for the necessary
     *           parameters to execute the CompleteMultipartUpload operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         CompleteMultipartUpload service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<CompleteMultipartUploadResult> completeMultipartUploadAsync(CompleteMultipartUploadRequest completeMultipartUploadRequest,
            AsyncHandler<CompleteMultipartUploadRequest, CompleteMultipartUploadResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes the access policy associated with the
     * specified vault. The operation is eventually consistent; that is, it
     * might take some time for Amazon Glacier to completely remove the
     * access policy, and you might still see the effect of the policy for a
     * short time after you send the delete request.
     * </p>
     * <p>
     * This operation is idempotent. You can invoke delete multiple times,
     * even if there is no policy associated with the vault. For more
     * information about vault access policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param deleteVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the DeleteVaultAccessPolicy operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultAccessPolicyAsync(DeleteVaultAccessPolicyRequest deleteVaultAccessPolicyRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes the access policy associated with the
     * specified vault. The operation is eventually consistent; that is, it
     * might take some time for Amazon Glacier to completely remove the
     * access policy, and you might still see the effect of the policy for a
     * short time after you send the delete request.
     * </p>
     * <p>
     * This operation is idempotent. You can invoke delete multiple times,
     * even if there is no policy associated with the vault. For more
     * information about vault access policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param deleteVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the DeleteVaultAccessPolicy operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultAccessPolicyAsync(DeleteVaultAccessPolicyRequest deleteVaultAccessPolicyRequest,
            AsyncHandler<DeleteVaultAccessPolicyRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation uploads a part of an archive. You can upload archive
     * parts in any order. You can also upload them in parallel. You can
     * upload up to 10,000 parts for a multipart upload.
     * </p>
     * <p>
     * Amazon Glacier rejects your upload part request if any of the
     * following conditions is true:
     * </p>
     * 
     * <ul>
     * <li> <p>
     * <b>SHA256 tree hash does not match</b> To ensure that part data is
     * not corrupted in transmission, you compute a SHA256 tree hash of the
     * part and include it in your request. Upon receiving the part data,
     * Amazon Glacier also computes a SHA256 tree hash. If these hash values
     * don't match, the operation fails. For information about computing a
     * SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * .
     * </p>
     * </li>
     * <li> <p>
     * <b>Part size does not match</b> The size of each part except the last
     * must match the size specified in the corresponding
     * InitiateMultipartUpload request. The size of the last part must be the
     * same size as, or smaller than, the specified size.
     * </p>
     * <p>
     * <b>NOTE:</b> If you upload a part whose size is smaller than the part
     * size you specified in your initiate multipart upload request and that
     * part is not the last part, then the upload part request will succeed.
     * However, the subsequent Complete Multipart Upload request will fail.
     * </p>
     * </li>
     * <li> <b>Range does not align</b> The byte range value in the request
     * does not align with the part size specified in the corresponding
     * initiate request. For example, if you specify a part size of 4194304
     * bytes (4 MB), then 0 to 4194303 bytes (4 MB - 1) and 4194304 (4 MB) to
     * 8388607 (8 MB - 1) are valid part ranges. However, if you set a range
     * value of 2 MB to 6 MB, the range does not align with the part size and
     * the upload will fail. </li>
     * 
     * </ul>
     * <p>
     * This operation is idempotent. If you upload the same part multiple
     * times, the data included in the most recent request overwrites the
     * previously uploaded data.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-upload-part.html"> Upload Part </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param uploadMultipartPartRequest Container for the necessary
     *           parameters to execute the UploadMultipartPart operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         UploadMultipartPart service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<UploadMultipartPartResult> uploadMultipartPartAsync(UploadMultipartPartRequest uploadMultipartPartRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation uploads a part of an archive. You can upload archive
     * parts in any order. You can also upload them in parallel. You can
     * upload up to 10,000 parts for a multipart upload.
     * </p>
     * <p>
     * Amazon Glacier rejects your upload part request if any of the
     * following conditions is true:
     * </p>
     * 
     * <ul>
     * <li> <p>
     * <b>SHA256 tree hash does not match</b> To ensure that part data is
     * not corrupted in transmission, you compute a SHA256 tree hash of the
     * part and include it in your request. Upon receiving the part data,
     * Amazon Glacier also computes a SHA256 tree hash. If these hash values
     * don't match, the operation fails. For information about computing a
     * SHA256 tree hash, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/checksum-calculations.html"> Computing Checksums </a>
     * .
     * </p>
     * </li>
     * <li> <p>
     * <b>Part size does not match</b> The size of each part except the last
     * must match the size specified in the corresponding
     * InitiateMultipartUpload request. The size of the last part must be the
     * same size as, or smaller than, the specified size.
     * </p>
     * <p>
     * <b>NOTE:</b> If you upload a part whose size is smaller than the part
     * size you specified in your initiate multipart upload request and that
     * part is not the last part, then the upload part request will succeed.
     * However, the subsequent Complete Multipart Upload request will fail.
     * </p>
     * </li>
     * <li> <b>Range does not align</b> The byte range value in the request
     * does not align with the part size specified in the corresponding
     * initiate request. For example, if you specify a part size of 4194304
     * bytes (4 MB), then 0 to 4194303 bytes (4 MB - 1) and 4194304 (4 MB) to
     * 8388607 (8 MB - 1) are valid part ranges. However, if you set a range
     * value of 2 MB to 6 MB, the range does not align with the part size and
     * the upload will fail. </li>
     * 
     * </ul>
     * <p>
     * This operation is idempotent. If you upload the same part multiple
     * times, the data included in the most recent request overwrites the
     * previously uploaded data.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/uploading-archive-mpu.html"> Uploading Large Archives in Parts (Multipart Upload) </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-upload-part.html"> Upload Part </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param uploadMultipartPartRequest Container for the necessary
     *           parameters to execute the UploadMultipartPart operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         UploadMultipartPart service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<UploadMultipartPartResult> uploadMultipartPartAsync(UploadMultipartPartRequest uploadMultipartPartRequest,
            AsyncHandler<UploadMultipartPartRequest, UploadMultipartPartResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation removes one or more tags from the set of tags attached
     * to a vault. For more information about tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * . This operation is idempotent. The operation will be successful,
     * even if there are no tags attached to the vault.
     * </p>
     *
     * @param removeTagsFromVaultRequest Container for the necessary
     *           parameters to execute the RemoveTagsFromVault operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         RemoveTagsFromVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> removeTagsFromVaultAsync(RemoveTagsFromVaultRequest removeTagsFromVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation removes one or more tags from the set of tags attached
     * to a vault. For more information about tags, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/tagging.html"> Tagging Amazon Glacier Resources </a>
     * . This operation is idempotent. The operation will be successful,
     * even if there are no tags attached to the vault.
     * </p>
     *
     * @param removeTagsFromVaultRequest Container for the necessary
     *           parameters to execute the RemoveTagsFromVault operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         RemoveTagsFromVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> removeTagsFromVaultAsync(RemoveTagsFromVaultRequest removeTagsFromVaultRequest,
            AsyncHandler<RemoveTagsFromVaultRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists in-progress multipart uploads for the specified
     * vault. An in-progress multipart upload is a multipart upload that has
     * been initiated by an InitiateMultipartUpload request, but has not yet
     * been completed or aborted. The list returned in the List Multipart
     * Upload response has no guaranteed order.
     * </p>
     * <p>
     * The List Multipart Uploads operation supports pagination. By default,
     * this operation returns up to 1,000 multipart uploads in the response.
     * You should always check the response for a <code>marker</code> at
     * which to continue the list; if there are no more items the
     * <code>marker</code> is <code>null</code> . To return a list of
     * multipart uploads that begins at a specific upload, set the
     * <code>marker</code> request parameter to the value you obtained from a
     * previous List Multipart Upload request. You can also limit the number
     * of uploads returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * Note the difference between this operation and listing parts
     * (ListParts). The List Multipart Uploads operation lists all multipart
     * uploads for a vault and does not require a multipart upload ID. The
     * List Parts operation requires a multipart upload ID since parts are
     * associated with a single upload.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-list-uploads.html"> List Multipart Uploads </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listMultipartUploadsRequest Container for the necessary
     *           parameters to execute the ListMultipartUploads operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         ListMultipartUploads service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListMultipartUploadsResult> listMultipartUploadsAsync(ListMultipartUploadsRequest listMultipartUploadsRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation lists in-progress multipart uploads for the specified
     * vault. An in-progress multipart upload is a multipart upload that has
     * been initiated by an InitiateMultipartUpload request, but has not yet
     * been completed or aborted. The list returned in the List Multipart
     * Upload response has no guaranteed order.
     * </p>
     * <p>
     * The List Multipart Uploads operation supports pagination. By default,
     * this operation returns up to 1,000 multipart uploads in the response.
     * You should always check the response for a <code>marker</code> at
     * which to continue the list; if there are no more items the
     * <code>marker</code> is <code>null</code> . To return a list of
     * multipart uploads that begins at a specific upload, set the
     * <code>marker</code> request parameter to the value you obtained from a
     * previous List Multipart Upload request. You can also limit the number
     * of uploads returned in the response by specifying the
     * <code>limit</code> parameter in the request.
     * </p>
     * <p>
     * Note the difference between this operation and listing parts
     * (ListParts). The List Multipart Uploads operation lists all multipart
     * uploads for a vault and does not require a multipart upload ID. The
     * List Parts operation requires a multipart upload ID since parts are
     * associated with a single upload.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and the underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/working-with-archives.html"> Working with Archives in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-multipart-list-uploads.html"> List Multipart Uploads </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param listMultipartUploadsRequest Container for the necessary
     *           parameters to execute the ListMultipartUploads operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         ListMultipartUploads service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<ListMultipartUploadsResult> listMultipartUploadsAsync(ListMultipartUploadsRequest listMultipartUploadsRequest,
            AsyncHandler<ListMultipartUploadsRequest, ListMultipartUploadsResult> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation sets and then enacts a data retrieval policy in the
     * region specified in the PUT request. You can set one policy per region
     * for an AWS account. The policy is enacted within a few minutes of a
     * successful PUT operation.
     * </p>
     * <p>
     * The set policy operation does not affect retrieval jobs that were in
     * progress before the policy was enacted. For more information about
     * data retrieval policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/data-retrieval-policy.html"> Amazon Glacier Data Retrieval Policies </a>
     * .
     * </p>
     *
     * @param setDataRetrievalPolicyRequest Container for the necessary
     *           parameters to execute the SetDataRetrievalPolicy operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         SetDataRetrievalPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setDataRetrievalPolicyAsync(SetDataRetrievalPolicyRequest setDataRetrievalPolicyRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation sets and then enacts a data retrieval policy in the
     * region specified in the PUT request. You can set one policy per region
     * for an AWS account. The policy is enacted within a few minutes of a
     * successful PUT operation.
     * </p>
     * <p>
     * The set policy operation does not affect retrieval jobs that were in
     * progress before the policy was enacted. For more information about
     * data retrieval policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/data-retrieval-policy.html"> Amazon Glacier Data Retrieval Policies </a>
     * .
     * </p>
     *
     * @param setDataRetrievalPolicyRequest Container for the necessary
     *           parameters to execute the SetDataRetrievalPolicy operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         SetDataRetrievalPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setDataRetrievalPolicyAsync(SetDataRetrievalPolicyRequest setDataRetrievalPolicyRequest,
            AsyncHandler<SetDataRetrievalPolicyRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes a vault. Amazon Glacier will delete a vault
     * only if there are no archives in the vault as of the last inventory
     * and there have been no writes to the vault since the last inventory.
     * If either of these conditions is not satisfied, the vault deletion
     * fails (that is, the vault is not removed) and Amazon Glacier returns
     * an error. You can use DescribeVault to return the number of archives
     * in a vault, and you can use
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-initiate-job-post.html"> Initiate a Job (POST jobs) </a> to initiate a new inventory retrieval for a vault. The inventory contains the archive IDs you use to delete archives using <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-delete.html"> Delete Archive (DELETE archive) </a>
     * .
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-vaults.html"> Deleting a Vault in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-delete.html"> Delete Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param deleteVaultRequest Container for the necessary parameters to
     *           execute the DeleteVault operation on AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultAsync(DeleteVaultRequest deleteVaultRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation deletes a vault. Amazon Glacier will delete a vault
     * only if there are no archives in the vault as of the last inventory
     * and there have been no writes to the vault since the last inventory.
     * If either of these conditions is not satisfied, the vault deletion
     * fails (that is, the vault is not removed) and Amazon Glacier returns
     * an error. You can use DescribeVault to return the number of archives
     * in a vault, and you can use
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-initiate-job-post.html"> Initiate a Job (POST jobs) </a> to initiate a new inventory retrieval for a vault. The inventory contains the archive IDs you use to delete archives using <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-archive-delete.html"> Delete Archive (DELETE archive) </a>
     * .
     * </p>
     * <p>
     * This operation is idempotent.
     * </p>
     * <p>
     * An AWS account has full permission to perform all operations
     * (actions). However, AWS Identity and Access Management (IAM) users
     * don't have any permissions by default. You must grant them explicit
     * permission to perform specific actions. For more information, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/using-iam-with-amazon-glacier.html"> Access Control Using AWS Identity and Access Management (IAM) </a>
     * .
     * </p>
     * <p>
     * For conceptual information and underlying REST API, go to
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/deleting-vaults.html"> Deleting a Vault in Amazon Glacier </a> and <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/api-vault-delete.html"> Delete Vault </a>
     * in the <i>Amazon Glacier Developer Guide</i> .
     * </p>
     *
     * @param deleteVaultRequest Container for the necessary parameters to
     *           execute the DeleteVault operation on AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         DeleteVault service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> deleteVaultAsync(DeleteVaultRequest deleteVaultRequest,
            AsyncHandler<DeleteVaultRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation configures an access policy for a vault and will
     * overwrite an existing policy. To configure a vault access policy, send
     * a PUT request to the <code>access-policy</code> subresource of the
     * vault. An access policy is specific to a vault and is also called a
     * vault subresource. You can set one access policy per vault and the
     * policy can be up to 20 KB in size. For more information about vault
     * access policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param setVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the SetVaultAccessPolicy operation on
     *           AmazonGlacier.
     * 
     * @return A Java Future object containing the response from the
     *         SetVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setVaultAccessPolicyAsync(SetVaultAccessPolicyRequest setVaultAccessPolicyRequest) 
            throws AmazonServiceException, AmazonClientException;

    /**
     * <p>
     * This operation configures an access policy for a vault and will
     * overwrite an existing policy. To configure a vault access policy, send
     * a PUT request to the <code>access-policy</code> subresource of the
     * vault. An access policy is specific to a vault and is also called a
     * vault subresource. You can set one access policy per vault and the
     * policy can be up to 20 KB in size. For more information about vault
     * access policies, see
     * <a href="http://docs.aws.amazon.com/amazonglacier/latest/dev/vault-access-policy.html"> Amazon Glacier Access Control with Vault Access Policies </a>
     * .
     * </p>
     *
     * @param setVaultAccessPolicyRequest Container for the necessary
     *           parameters to execute the SetVaultAccessPolicy operation on
     *           AmazonGlacier.
     * @param asyncHandler Asynchronous callback handler for events in the
     *           life-cycle of the request. Users could provide the implementation of
     *           the four callback methods in this interface to process the operation
     *           result or handle the exception.
     * 
     * @return A Java Future object containing the response from the
     *         SetVaultAccessPolicy service method, as returned by AmazonGlacier.
     * 
     *
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client while
     *             attempting to make the request or handle the response.  For example
     *             if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonGlacier indicating
     *             either a problem with the data in the request, or a server side issue.
     */
    public Future<Void> setVaultAccessPolicyAsync(SetVaultAccessPolicyRequest setVaultAccessPolicyRequest,
            AsyncHandler<SetVaultAccessPolicyRequest, Void> asyncHandler)
                    throws AmazonServiceException, AmazonClientException;
}
        