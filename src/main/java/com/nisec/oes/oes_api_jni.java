package com.nisec.oes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Poolable;
import stormpot.Slot;

public class oes_api_jni {
	private static Logger LOGGER = LoggerFactory.getLogger(oes_api_jni.class);

    /**************************错误码定义,需实现者补充**************************/

	public final static int OES_OK    = 0x00000000;

	//掩码
	public final static int OES_MASK_HEADER						= 0xFF000000;	//验章码标签掩码
	public final static int OES_MASK_SEALVERIFY_STATE			= 0x000000F0;	//本地验章状态掩码
	public final static int OES_MASK_SEALVERIFY_ECODE			= 0x0000000F;	//本地验章错误码掩码
	public final static int OES_MASK_ONLINESEALVERIFY_STATE		= 0x0000F000;	//在线验章状态掩码
	public final static int OES_MASK_ONLINESEALVERIFY_ECODE		= 0x00000F00;	//在线验章错误码掩码
	public final static int OES_MASK_DOCVERIFY_STATE			= 0x00F00000;	//文档验证状态掩码
	public final static int OES_MASK_DOCVERIFY_ECODE			= 0x000F0000;	//文档验证错误码掩码

	//验章码标签
	public final static int OES_SEAL_HEADER			    		= 0xE0000000;  //验章码标签

	//本地验章
	public final static int OES_SEAL_VERIFY_ABORT	    		= 0x00000010;  //本地签章验证过程中断（不作为返回码）
	public final static int OES_SEAL_VERIFY_FAILURE				= 0x00000020;  //本地签章验证失败（不作为返回码）


	//在线验章
	public final static int OES_SEAL_ONLINE_VERIFY_ABORT		= 0x00001000; //在线签章验证过程中断（不作为返回码）
	public final static int OES_SEAL_ONLINE_VERIFY_FAILURE		= 0x00002000; //在线签章验证失败（不作为返回码）

	//文档验证
	public final static int OES_DOC_VERIFY_ABORT			  	= 0x00100000; //文档验证过程中断（不作为返回码）
	public final static int OES_DOC_VERIFY_FAILURE				= 0x00200000; //文档验证失败（不作为返回码）

	//文档签章，与章相关的错误与验章共用
	public final static int OES_DOC_SIGN_ABORT					= 0x00900000;  //文档签章过程中断（不作为返回码）
	public final static int OES_DOC_SIGN_FAILURE				= 0x00A00000;  //文档签章失败（不作为返回码）

	//使用方法(举例：错误码为0xE0211125)：
	//1.首先判断是否为OES_OK，是OK，否则转2（不是OES_OK，需继续判断错误）
	//2.将错误码与OES_MASK_HEADER进行&操作，将结果与OES_SEAL_HEADER进行比对：（结果为0xE0000000与OES_SEAL_HEADER相等，转到1））
	//  1) 如相等，继续解释错误码，转3；
	//  2) 否则，参照相关系统错误编码解析；
	//3.将错误码与状态掩码进行&操作,可获得错误的状态；（结果为0x00201020）
	//4.通过错误码调用OES_GetVerifyErrMessage()获得错误信息。（文档验证失败-...；在线签章验证过程中断-...；在线签章失败-盖章时过期）


	//以下错误供DLL内部使用，调用方无需关心只需通过调用OES_GetVerifyErrMessage()获得错误信息即可。
	//#define OES_SEAL_ONLINE_VERIFY_ABORT		0x00001100  //在线签章验证过程中断-...
	public final static int OES_SEAL_ONLINE_VERIFY_NOTEXIST		= 0x00002100; //在线签章-不存在
	public final static int OES_SEAL_ONLINE_VERIFY_LOST			= 0x00002200; //在线签章-盖章时挂失
	public final static int OES_SEAL_ONLINE_VERIFY_REVOKE		= 0x00002300; //在线签章-盖章时注销
	public final static int OES_SEAL_ONLINE_VERIFY_LOCKED		= 0x00002400; //在线签章-盖章时停用
	public final static int OES_SEAL_ONLINE_VERIFY_EXPIRED		= 0x00002500; //在线签章-盖章时过期
	public final static int OES_SEAL_ONLINE_VERIFY_OTHER		= 0x00002F00; //在线签章验证其它失败


	/**
	 * @brief 返回签章算法提供者信息 [Required]
	 * @param[out]     puchName      名称（UTF-8编码），当为NULL时，通过piNameLen给出长度
	 * @param[out/in]  piNameLen     名称长度
	 * @param[out]     puchCompany   公司名称（UTF-8编码），当为NULL时，通过piCompanyLen给出长度
	 * @param[out/in]  piCompanyLen  公司名称长度
	 * @param[out]     puchVersion   版本（UTF-8编码），当为NULL时，通过piVersionLen给出长度
	 * @param[out/in]  piVersionLen  版本长度
	 * @param[out]     puchExtend    扩展信息（UTF-8编码），当为NULL时，通过piExtendLen给出长度
	 * @param[out/in]  piExtendLen   扩展信息长度
	 * @return 调用成功返回OES_OK，否则是错误代码
	 */
	public native int  OES_GetProviderInfo(byte[] uchName   , int[] iNameLen,
										   byte[] uchCompany, int[] iCompanyLen,
										   byte[] uchVersion, int[] iVersionLen,
										   byte[] uchExtend,  int[] iExtendLen);

	/**

	 * @brief 获取用于签章列表，该函数可用来进行Name到ID的转换  [Required]

	 * @param[out]     puchSealListData            印章列表数据（UTF-8编码），当为NULL时，通过piSealListDataLen给出长度

	 *                                             形如 ID1\0Name1\0ID2\0Name2\0\0

	 * @param[out/in]  piSealListDataLen           印章列表数据长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int  OES_GetSealList(byte[] uchSealListData,int[] iSealListDataLen);


	/**

	 * @brief 获取用于签章的印模结构（含印模图像、校验证书和其他数据,符合国密标准）  [Required]

	 * @param[in]      puchSealId              印章标识或名称（字符串）

	 * @param[in]      iSealIdLen              印章标识或名称长度

	 * @param[out]     puchSealData            印章数据（符合国密标准），当为NULL时，通过piSealListDataLen给出长度

	 * @param[out/in]  piSealDataLen           印章数据长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int  OES_GetSeal(byte[] uchSealId, int iSealIdLen,
								   byte[] uchSealData,int[] iSealDataLen);



	/**
 * @brief 获取印章图像及尺寸  [Required]
 * @param[in]      puchSealData             印章数据（符合国密标准），当为NULL时，通过piSealListDataLen给出长度
 * @param[in]      iSealDataLen             印章数据长度
 * @param[in]      iRenderFlag              绘制用途标记，0表示显示，1表示打印，2表示预览
 * @param[out]     puchSealImage            印章图像数据，当为NULL时，通过piSealImageLen给出长度
 * @param[out/in]  piSealImageLen           印章图像数据长度
 * @param[out/in]  piSealWidth              印章宽度（单位mm）
 * @param[out/in]  piSealHeight             印章高度（单位mm）
 * @return 调用成功返回OES_OK，否则是错误代码
 */
	public native int  OES_GetSealImage(byte[] puchSealData,int iSealDataLen,int iRenderFlag,
														byte[] puchSealImage,int[] piSealImageLen,int[] piSealWidth,int[] piSealHeight);

	/**

	 * @brief 获取用于签章的印模结构（含印模图像、校验证书和其他数据,符合国密标准）  [Required]

	 * @param[in]  puchSealData           印章数据（符合国密标准）

	 * @param[in]  iSealDataLen           印章数据长度

	 * @param[out]     puchSealId             头信息-印章标识，当为NULL时，通过piSealIdLen给出长度

	 * @param[out/in]  piSealIdLen            头信息-印章标识长度

	 * @param[out]     puchVersion            头信息-版本，当为NULL时，通过piVersionLen给出长度

	 * @param[out/in]  piVersionLen           头信息-版本数据长度

	 * @param[out]     puchVenderId           头信息-厂商标识，当为NULL时，通过piVenderIdLen给出长度

	 * @param[out/in]  piVenderIdLen          头信息-厂商标识长度

	 * @param[out]     puchSealType           印章信息-印章类型，当为NULL时，通过piSealTypeLen给出长度

	 * @param[out/in]  piSealTypeLen          印章信息-印章类型长度

	 * @param[out]     puchSealName           印章信息-印章名称，当为NULL时，通过piSealNameLen给出长度

	 * @param[out/in]  piSealNameLen          印章信息-印章名称长度

	 * @param[out]     puchCertInfo           印章信息-证书列表信息，当为NULL时，通过piCertInfoLen给出长度

	 * @param[out/in]  piCertInfoLen          印章信息-证书列表信息长度

	 * @param[out]     puchValidStart         印章信息-有效起始时间，当为NULL时，通过piValidStartLen给出长度

	 * @param[out/in]  piValidStartLen        印章信息-有效起始时间长度

	 * @param[out]     puchValidEnd           印章信息-有效结束时间，当为NULL时，通过piValidEndLen给出长度

	 * @param[out/in]  piValidEndLen          印章信息-有效结束长度

	 * @param[out]     puchSignedDate         印章信息-制作日期，当为NULL时，通过piSignedDateLen给出长度

	 * @param[out/in]  piSignedDateLen        印章信息-制作日期长度

	 * @param[out]     puchSignerName         签名信息-制章人，当为NULL时，通过piSignerNameLen给出长度

	 * @param[out/in]  piSignerNameLen        签名信息-制章人长度

	 * @param[out]     puchSignMethod         签名信息-制章签名方法，当为NULL时，通过piSignMethodLen给出长度

	 * @param[out/in]  piSignMethodLen        签名信息-制章签名方法长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int  OES_GetSealInfo( byte[] uchSealData,		int   iSealDataLen,
	                                    byte[] uchSealId,  		int[] iSealIdLen,
	                                    byte[] uchVersion, 		int[] iVersionLen,
	                                    byte[] uchVenderId,		int[] iVenderIdLen,
	                                    byte[] uchSealType,		int[] iSealTypeLen,
	                                    byte[] uchSealName,		int[] iSealNameLen,
	                                    byte[] uchCertInfo,     int[] iCertInfoLen,
	                                    byte[] uchValidStart,   int[] iValidStartLen,
	                                    byte[] uchValidEnd,		int[] iValidEndLen,
	                                    byte[] uchSignedDate,   int[] iSignedDateLen,
	                                    byte[] uchSignerName,	int[] iSignerNameLen,
										byte[] uchSignMethod,	int[] iSignMethodLen);


	/**

	 * @brief 获取签名时间（时间戳或明文形式） [Required]

	 * @param[out]     puchSignDateTime          签名时间（字符时用UTF-8编码，形如yyyy-MM-dd hh:mm:ss,时间戳时二进制值），当为NULL时，通过piSignDateTimeLen给出长度

	 * @param[out/in]  piSignDateTimeLen         签名时间长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int   OES_GetSignDateTime(byte[] uchSignDateTime,int[] iSignDateTimeLen);

	/**

	 * @brief 获取签名算法标识 [Required]

	 * @param[out]     puchSignMethod            签名算法（UTF-8编码），当为NULL时，通过piSignMethodLen给出长度

	 * @param[out/in]  piSignMethodLen           签名算法长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int    OES_GetSignMethod(byte[] uchSignMethod,int[] iSignMethodLen);


	/**

	 * @brief 获取摘要算法标识 [Required]

	 * @param[out]     puchDigestMethod          摘要算法（UTF-8编码），当为NULL时，通过piDigestMethodLen给出长度

	 * @param[out/in]  piDigestMethodLen         摘要算法长度

	 * @return 调用成功返回OES_OK，否则是错误代码

	 */

	public native int   OES_GetDigestMethod(byte[] uchDigestMethod,int[] iDigestMethodLen);




	/**

	 * @brief 代理计算摘要  [Required]

	 * @param[in]      puchData                  待摘要的数据

	 * @param[in]      iDataLen                  待摘要的数据长度

	 * @param[in]      puchDigestMethod          摘要算法

	 * @param[in]      iDigestMethodLen          摘要算法长度

	 * @param[out]     puchDigestValue           摘要值，当为NULL时，通过piDigestValueLen给出长度

	 * @param[out/in]  piDigestValueLen          摘要值长度

	 * @return 调用成功返回OES_OK，否则是错误代码，可调用OES_GetErrMessage()获取详细信息。

	 */

	public native long    OES_Digest(byte[] uchData,        int iDataLen,
	                                 byte[] uchDigestMethod,int iDigestMethodLen,
	                                 byte[] uchDigestValue, int[] iDigestValueLen);


	/**

	 * @brief 代理计算签名,如果计算前需要输入密码，插件实现者需要提供输入界面  [Required]

	 * @param[in]      puchSealId              印章标识

	 * @param[in]      iSealIdLen              印章标识长度

	 * @param[in]      puchDocProperty         文档信息，固定为Signature.xml的绝对路径

	 * @param[in]      iDocPropertyLen         文档信息长度

	 * @param[in]      puchDigestData          摘要数据

	 * @param[in]      iDigestDataLen          摘要数据长度

	 * @param[in]      puchSignMethod          签名算法

	 * @param[in]      iSignMethodLen          签名算法长度

	 * @param[in]      puchSignDateTime        签名时间

	 * @param[in]      iSignDateTimeLen        签名时间长度

	 * @param[out]     puchSignValue           签名值（符合国密标准），当为NULL时，通过piSignValueLen给出长度

	 * @param[out/in]  piSignValueLen          签名值长度

	 * @return 调用成功返回OES_OK，否则是错误代码，可调用OES_GetErrMessage()获取详细信息。

	 */

	public native long    OES_Sign(byte[] uchSealId,      int iSealIdLen,
	                               byte[] uchDocProperty, int iDocPropertyLen,
	                               byte[] uchDigestData,  int iDigestDataLen,
	                               byte[] uchSignMethod,  int iSignMethodLen,
	                               byte[] uchSignDateTime,int iSignDateTimeLen,
	                               byte[] uchSignValue,   int[] iSignValueLen);



	/**

	 * @brief 代理验签实现  [Required]

	 * @param[in]  puchSealData            印章数据

	 * @param[in]  iSealDataLen            印章数据长度

	 * @param[in]  puchDocProperty         文档信息，固定为Signature.xml的绝对路径

	 * @param[in]  iDocPropertyLen         文档信息长度

	 * @param[in]  puchSignMethod          签名算法

	 * @param[in]  iSignMethodLen          签名算法长度

	 * @param[in]  puchSignDateTime        签名时间

	 * @param[in]  piSignDateTimeLen       签名时间长度

	 * @param[in]  puchSignValue           签名值

	 * @param[in]  iSignValueLen           签名值长度

	 * @param[in]  iOnline                 是否在线验证

	 * @return 调用成功返回OES_OK，否则是错误代码，返回值包括三段（印章本地验证结果，印章在线验证结果，文档验证结果），

	 *         在发生错误时，详细的错误信息需要调用OES_GetErrMessage()。

	 */

	public native long    OES_Verify(   byte[] uchSealData,    int iSealDataLen,
	                                    byte[] uchDocProperty, int iDocPropertyLen,
	                                    byte[] uchDigestData,  int iDigestDataLen,
	                                    byte[] uchSignMethod,  int iSignMethodLen,
	                                    byte[] uchSignDateTime,int iSignDateTimeLen,
	                                    byte[] uchSignValue,   int iSignValueLen,
	                                    int iOnline);



	/**

	 * @brief 取得错误信息

	 * @param[in]      errCode              错误代码，获得于OES_Verify()或者OES_Sign()、OES_Digest()

	 * @param[out]     puchErrMessage       错误信息（UTF-8编码），应使用UTF-8编码，当为NULL时，通过piErrMessageLen给出长度

	 * @param[out/in]  piErrMessageLen      错误信息长度

	*/

	public native int  OES_GetErrMessage( long errCode,byte[] uchErrMessage,int[] iErrMessageLen);

	public native static void OES_xmlInitParser();

	public native static void OES_xmlCleanupParser();


    public static boolean OES_Init() {
        try {
            Runtime.getRuntime().load("/oes/lib/libOesJni.so");
        } catch (SecurityException se) {
            LOGGER.error("Security Error when loading libOesJni.so.", se);
            return false;
        } catch (UnsatisfiedLinkError un) {
            LOGGER.error("UnsatisfiedLinkError Error when loading libOesJni.so.", un);
            return false;
        }
        LOGGER.info("Load libOesJni.so is successful");
        oes_api_jni.OES_xmlInitParser();
        return true;

    }

    public static void OES_Destory()
	{
		oes_api_jni.OES_xmlCleanupParser();
	}
	/*
	private  boolean loadlibrary()
	{
		  try{ 			
		  		Runtime.getRuntime().load("/oes/lib/libOesJni.so");
								  
              //  Runtime.getRuntime().loadLibrary("SDF"); 
                }
                catch(SecurityException se){
                	
                        System.out.println("error1\n");	
                        return false;
                }
                catch(UnsatisfiedLinkError un){
                		
                        System.out.println("error2\n");
                        System.out.println(un.getMessage());
                        return false;
                }
                System.out.println( "succese\n");
                return true;
	}

*/

    public oes_api_jni() {
    }
}
