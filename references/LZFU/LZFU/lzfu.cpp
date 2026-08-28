#include <iostream>
#include <vector>
#include "lzfu.h"

#define RTFLEN sizeof(LZFU)

#define MAGIC_UNCOMPRESSED 0x414c454d
#define MAGIC_COMPRESSED 0x75465a4c

int DICT_SIZE = 4096;
int DICT_MASK = DICT_SIZE-1;

unsigned char RTF_PREBUF[] =
	"{\\rtf1\\ansi\\mac\\deff0\\deftab720{\\fonttbl;}" \
	"{\\f0\\fnil \\froman \\fswiss \\fmodern \\fscript " \
	"\\fdecor MS Sans SerifSymbolArialTimes New RomanCourier" \
	"{\\colortbl\\red0\\green0\\blue0\n\r\\par " \
	"\\pard\\plain\\f0\\fs20\\b\\i\\u\\tab\\tx";
	
template <typename T>
__forceinline uint32_t getU32(T buf, int &pos)
{
	uint32_t ret = ((buf[pos] & 0xFF) | (buf[pos + 1] & 0xFF) << 8 |
		(buf[pos + 2] & 0xFF) << 16 | (buf[pos + 3] & 0xFF) << 24) & 0xFFFFFFFFL;

	pos += 4;
	return ret;
}

template<typename T>
int _bcpy(T dst, int dst_off, T src, int src_off, int sz)
{
	for(int i = 0; i < sz; i++)
		dst[dst_off + i] = src[src_off + i];
	
	return dst_off + sz;
}

std::vector<unsigned char> inflateRTF(unsigned char *src)
{
	std::vector<unsigned char> v;
	unsigned char *dst = nullptr;

	if (src == nullptr || RTFLEN < 16)
		return v;
	
	int in = 0, out = 0;
	int compressedSize = getU32(src, in); 
	int uncompressedSize = getU32(src, in); 
	int magic = getU32(src, in); 
	int crc32 = getU32(src, in); 
	
	if (compressedSize == RTFLEN - 4)
	{
		if (uncompressedSize != 0)
		{
			if (magic == MAGIC_UNCOMPRESSED) 
			{
				dst = new unsigned char[uncompressedSize];
				out = _bcpy(dst, out, src, in, uncompressedSize);
			}
			else if (magic = MAGIC_COMPRESSED) 
				dst = new unsigned char[sizeof(RTF_PREBUF) - 1 + uncompressedSize];
			{
				dst = new unsigned char[sizeof(RTF_PREBUF) - 1 + uncompressedSize];
				out = _bcpy(dst, out, RTF_PREBUF, 0, sizeof(RTF_PREBUF) - 1);
				
				int flagCount = 0;
				int flags = 0;
				while (true)
				{
					flags = ((flagCount++ & 7) == 0) ? src[in++] : flags >> 1;

					if ((flags & 1) == 0) 
						dst[out++] = src[in++];
					else 
					{
						int offset = src[in++] & 0xFF,
							length = src[in++] & 0xFF;
						
						offset = (offset << 4) | (length >> 4);
						length = (length & 0xF) + 2;
						
						offset = out & ~DICT_MASK | offset;			
						if(offset >= out) {
							if (offset == out)
								break;
							offset -= DICT_SIZE;
						}
						
						int end = offset + length;
						while (offset < end)
							dst[out++] = dst[offset++];
					}
				}
				
				src = dst; 
				dst = new unsigned char[uncompressedSize];
				_bcpy(dst, 0, src, sizeof(RTF_PREBUF) - 1, uncompressedSize);

				delete[] src;
			}
		}

		for (int i = 0; i < uncompressedSize; i++)
			v.push_back(dst[i]);

		delete[] dst;
	}

	return v;
}

int main(int argc, char **argv)
{
	auto inflated = inflateRTF2(LZFU);

	for (auto i = inflated.begin(); i != inflated.end(); ++i)
		std::cout << *i;

	std::cout << std::endl;

	return 0;
}
