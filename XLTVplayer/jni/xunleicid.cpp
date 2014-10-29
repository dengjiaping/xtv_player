#ifndef __USE_FILE_OFFSET64
#define __USE_FILE_OFFSET64
#endif

#ifndef __USE_LARGEFILE64
#define __USE_LARGEFILE64
#endif

#ifndef _LARGEFILE64_SOURCE
#define _LARGEFILE64_SOURCE
#endif

#include <stdint.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <errno.h>

#include <android/log.h>

#include "xunleicid.h"
#include "sha1.h"

#define XL_DEBUG 1
#define LOG_TAG "xunleicid"

#if XL_DEBUG
#define LOGD(...) (__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))
#else
#define LOGD(...)
#endif

/*
 * in: 待加密的字节数组
 * in_len: 待加密的字节数组长度
 * out: 加密后的输出字节，经过定义的规则转换后的结果，这里取了44位输出结果，因此定义此数组的时候长度需>44，建议设成60
 */
void do_sha1(unsigned char* in, int in_len, char* out) {
    const int CID_SIZE = 20;
    unsigned char cid_out[CID_SIZE];

    sha1_context sha;
    sha1_starts(&sha);
    sha1_update(&sha, (const unsigned char*)in, (uint32_t) in_len);
    sha1_finish(&sha, cid_out);

    uint32_t offset = 0;
    offset = sprintf(out, "cid:");
    for (unsigned i = 0; i < CID_SIZE; ++i) {
        offset += sprintf(out+offset, "%02X", (unsigned char) cid_out[i]);
    }
    out[offset] = '\0';
}

JNIEXPORT jstring JNICALL Java_com_kankan_player_util_CidUtil_nativeQueryCidByPath
  (JNIEnv *env, jclass jobject, jstring jpath) {
    if (!jpath) {
        return NULL;
    }

    char *path = (char *) env->GetStringUTFChars(jpath, NULL);
    LOGD("file name is %s\n", path);
    int fd = open(path, O_RDONLY|O_LARGEFILE); //解决大文件读不出数据问题，增加O_LARGEFILE
    env->ReleaseStringUTFChars(jpath, path);

    if (fd < 0) {
        LOGD("can't open file %d", errno);
        return NULL;
    }

    struct stat64 file_stat;
    if (fstat64(fd, &file_stat) < 0 || file_stat.st_size <= 0) {
        close(fd);
        LOGD("can't stat file %d", errno);
        return NULL;
    }
    uint64_t filesize = file_stat.st_size;
    // 打印uint64_t, 32位系统使用%llu, 64位使用%lu
    LOGD("filesize = %llu", filesize);

    const int CID_PART_SIZE = (20 << 10);	// 20K
    unsigned char tmpbuffer[3 * CID_PART_SIZE];	// 60K
    int len = 0;

    if (filesize < uint64_t(3 * CID_PART_SIZE)) {	// 小文件读取
        ssize_t read_bytes = read(fd, tmpbuffer, filesize);
        len = filesize;
        if (read_bytes != (ssize_t) filesize) {
                close(fd);
                LOGD("can't read %llu bytes %d", filesize,errno);
                return NULL;
        }
    } else {	// 大文件读取
        // part 1
        ssize_t read_bytes = read(fd, tmpbuffer, CID_PART_SIZE);
        if (read_bytes != (ssize_t) CID_PART_SIZE) {
                close(fd);
                LOGD("can't read %d bytes (1) %d", CID_PART_SIZE, errno);
                return NULL;
        }

        // part 2
        if (lseek64(fd, (filesize / 3), SEEK_SET) == (off_t) -1) {	// 为何吧文件指针移动到文件大小的1/3处。
                close(fd);
                LOGD("can't seek (1) %d", errno);
                return NULL;
        }

        read_bytes = read(fd, tmpbuffer + CID_PART_SIZE, CID_PART_SIZE);
        if (read_bytes != (ssize_t) CID_PART_SIZE) {
            close(fd);
            LOGD("can't read %d bytes (2) %d", CID_PART_SIZE, errno);
            return NULL;
        }

        // part 3
        if (lseek64(fd, (filesize - CID_PART_SIZE), SEEK_SET) == (off_t) -1) {
            close(fd);
            LOGD("can't seek (2) %d", errno);
            return NULL;
        }

        read_bytes = read(fd, tmpbuffer + 2 * CID_PART_SIZE, CID_PART_SIZE);
        if (read_bytes != (ssize_t) CID_PART_SIZE) {
            close(fd);
            LOGD("can't read %d bytes (2) %d", CID_PART_SIZE, errno);
            return NULL;
        }

        len = 3 * CID_PART_SIZE;
    }

    close(fd);

    char buffer[60];
    do_sha1(tmpbuffer, len, buffer);

    return (env)->NewStringUTF(buffer);
}

JNIEXPORT jstring JNICALL Java_com_kankan_player_util_CidUtil_nativeQueryCidByData
  (JNIEnv *env, jclass jobject, jbyteArray jdata) {
    if (!jdata) {
        return NULL;
    }

    jbyte *data = env->GetByteArrayElements(jdata, NULL);
    if (!data) {
        LOGD("read byte array from jvm error.");
        return NULL;
    }

    int len = env->GetArrayLength(jdata);
    LOGD("len = %d", len);

    char buffer[60];
    do_sha1((unsigned char*)data, len, buffer);
    env->ReleaseByteArrayElements(jdata, data, NULL);

    return (env)->NewStringUTF(buffer);
}